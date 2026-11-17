/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package nabu.protocols.smtp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import nabu.protocols.smtp.client.types.EmailAttachment;
import nabu.protocols.smtp.client.types.SMTPClientInformation;

import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SMTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.smtp.EmailType;
import be.nabu.eai.module.smtp.LoginMethod;
import be.nabu.eai.module.smtp.SMTPClientArtifact;
import be.nabu.eai.repository.Notification;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.containers.chars.WritableStraightByteToCharContainer;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.MimeFormatter;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeMultiPart;
import be.nabu.utils.security.SSLContextType;

@WebService
public class Services {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private ExecutionContext executionContext;
	
	@WebResult(name = "part")
	public Part newEmailPart(@WebParam(name = "from") String from, @WebParam(name = "to") List<String> to, @WebParam(name = "cc") List<String> cc, @WebParam(name = "subject") String subject, @WebParam(name = "content") InputStream content, @WebParam(name = "type") EmailType type, @WebParam(name = "charset") Charset charset, @WebParam(name = "headers") List<Header> headers, @WebParam(name = "attachments") List<EmailAttachment> attachments) {
		if (type == null) {
			type = EmailType.HTML;
		}
		String contentType = type.getContentType();
		if (charset != null) {
			contentType += "; charset=" + charset.name().toLowerCase();
		}
		ModifiablePart part = new PlainMimeContentPart(null, IOUtils.wrap(content), 
			new MimeHeader("Content-Type", contentType)
		);
		if (attachments != null && !attachments.isEmpty()) {
			PlainMimeMultiPart multiPart = new PlainMimeMultiPart(null, new MimeHeader("Content-Type", "multipart/mixed"));
			// first we add the content part
			multiPart.addChild(part);
			// add the attachments
			for (EmailAttachment attachment : attachments) {
				PlainMimeContentPart attachmentPart = new PlainMimeContentPart(multiPart, IOUtils.wrap(attachment.getContent()), 
					new MimeHeader("Content-Type", attachment.getContentType() == null ? "application/octet-stream" : attachment.getContentType())
				);
				MimeHeader dispositionHeader = new MimeHeader("Content-Disposition", attachment.getInline() != null && attachment.getInline() ? "inline" : "attachment");
				if (attachment.getHeaders() != null) {
					for (Header header : attachment.getHeaders()) {
						attachmentPart.setHeader(header);
					}
				}
				if (attachment.getName() != null) {
					dispositionHeader.addComment("filename=\"" + attachment.getName() + "\"");
				}
				attachmentPart.setHeader(dispositionHeader);
				multiPart.addChild(attachmentPart);
			}
			part = multiPart;
		}
		if (from != null && !from.isEmpty()) {
			part.setHeader(new MimeHeader("From", from));
		}
		if (subject != null) {
			part.setHeader(new MimeHeader("Subject", subject));
		}
		if (to != null && !to.isEmpty()) {
			part.setHeader(new MimeHeader("To", join(to)));
		}
		if (cc != null && !cc.isEmpty()) {
			part.setHeader(new MimeHeader("Cc", join(cc)));
		}
		if (headers != null) {
			for (Header header : headers) {
				part.setHeader(header);
			}
		}
		part.setHeader(new MimeHeader("Mime-Version", "1.0"));
		return part;
	}
	
	static String invokeAccessTokenProvider(Service provider, String smtpClientId, String username, String from, String context, ExecutionContext executionContext) throws Exception {
		ComplexContent input = provider.getServiceInterface().getInputDefinition().newInstance();
		input.set("smtpClientId", smtpClientId);
		input.set("username", username);
		input.set("from", from);
		input.set("context", context);
		ComplexContent output = new ServiceRuntime(provider, executionContext).run(input);
		return output == null ? null : (String) output.get("accessToken");
	}

	private static String join(List<String> recipients) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < recipients.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(recipients.get(i));
		}
		return builder.toString();
	}
	
	@WebResult(name = "information")
	public SMTPClientInformation information(@NotNull @WebParam(name = "smtpClientId") String smtpClientId) throws IOException {
		SMTPClientArtifact smtp = executionContext.getServiceContext().getResolver(SMTPClientArtifact.class).resolve(smtpClientId);
		if (smtp == null) {
			throw new IllegalArgumentException("Could not find the smtp server: " + smtpClientId);
		}
		SMTPClientInformation information = new SMTPClientInformation();
		information.setBlacklist(smtp.getConfiguration().getBlacklist());
		information.setCharset(smtp.getConfiguration().getCharset());
		information.setClientHost(smtp.getConfiguration().getClientHost());
		information.setConnectionTimeout(smtp.getConfiguration().getConnectionTimeout());
		information.setHost(smtp.getConfiguration().getHost());
		information.setImplicitSSL(smtp.getConfiguration().getImplicitSSL());
		information.setPort(smtp.getConfiguration().getPort());
		information.setSocketTimeout(smtp.getConfiguration().getSocketTimeout());
		information.setUsername(smtp.getConfiguration().getUsername());
		return information;
	}
	
	public void send(@WebParam(name = "part") Part part, @WebParam(name = "from") String from, @WebParam(name = "to") @NotNull List<String> to, @WebParam(name = "smtpClientId") @NotNull String smtpClientId, @WebParam(name = "notifyOnFailure") Boolean notifyOnFailure, @WebParam(name = "quoteBoundary") Boolean quoteBoundary) throws Exception {
		if (part == null) {
			return;
		}
		// get the client
		SMTPClientArtifact smtp = executionContext.getServiceContext().getResolver(SMTPClientArtifact.class).resolve(smtpClientId);
		if (smtp == null) {
			throw new IllegalArgumentException("Could not find the smtp server: " + smtpClientId);
		}
		
		// update the subject if necessary (nice for test environments)
		if (part instanceof ModifiablePart && smtp.getConfig().getSubjectTemplate() != null && !smtp.getConfig().getSubjectTemplate().isEmpty()) {
			String template = smtp.getConfig().getSubjectTemplate();
			// if you did not specify a location to put the actual subject, we append it at the end
			if (!template.contains("${value}")) {
				template += " ${value}";
			}
			Header header = MimeUtils.getHeader("Subject", part.getHeaders());
			header = new MimeHeader("Subject", header == null || header.getValue() == null ? template.replace("${value}", "") : template.replace("${value}", header.getValue()));
			((ModifiablePart) part).removeHeader("Subject");
			((ModifiablePart) part).setHeader(header);
		}
		
		String xoauth2Stage = "preparation";
		try {
			// this is mostly for test purposes
			if (smtp.getConfig().getOverrideTo() != null && !smtp.getConfig().getOverrideTo().isEmpty()) {
				to = smtp.getConfig().getOverrideTo();
				// some servers override the smtp level rcpt to with the mime level to. this means even if we override the to, it still gets delivered to the original addresses
				// to prevent this, we can also override the to in the mime
				// to err on the side of caution, we set this default to true
				if (smtp.getConfig().getOverrideToInMime() == null || smtp.getConfig().getOverrideToInMime()) {
					((ModifiablePart) part).removeHeader("To");
					// also remove the cc or they might be added in as well
					((ModifiablePart) part).removeHeader("Cc");
					((ModifiablePart) part).setHeader(new MimeHeader("To", join(smtp.getConfig().getOverrideTo())));
				}
			}
			else if (smtp.getConfiguration().getBlacklist() != null) {
				// filter the "to" on the blacklist of the smtp artifact
				Iterator<String> recipient = to.iterator();
				while(recipient.hasNext()) {
					if (recipient.next().matches(smtp.getConfiguration().getBlacklist())) {
						recipient.remove();
					}
				}
				// if after blacklisting there are no "to" remaining, don't send the mail
				if (to.isEmpty()) {
					return;
				}
			}
			
			if (from == null) {
				from = smtp.getConfig().getFrom();
				if (from == null) {
					from = smtp.getConfiguration().getUsername();
				}
				if (from == null) {
					throw new IllegalArgumentException("No from given and did not find a username in the smtp artifact: " + smtpClientId);
				}
			}
			
			// TODO: make configurable?
			Header fromHeader = MimeUtils.getHeader("From", part.getHeaders());
			if (fromHeader == null && part instanceof ModifiablePart) {
				((ModifiablePart) part).setHeader(new MimeHeader("From", from));
			}
			
			// check if we want implicit ssl
			boolean implicitSSL = smtp.getConfiguration().getImplicitSSL() != null && smtp.getConfiguration().getImplicitSSL();
			boolean startTls = smtp.getConfiguration().getStartTls() != null && smtp.getConfiguration().getStartTls();
	
			// check if we have a configured keystore
			xoauth2Stage = "TLS configuration";
			KeyStoreArtifact keystore = smtp.getConfiguration().getKeystore();
			SSLContext context = keystore == null ? null : keystore.getKeyStore().newContext(SSLContextType.TLS);
			// use the default context if you have explicitly set the implicitSSL boolean
			if ((implicitSSL || startTls) && context == null) {
				context = SSLContext.getDefault();
			}
			
			// check the port
			int port;
			if (smtp.getConfiguration().getPort() == null) {
				port = implicitSSL ? 465 : 25;
			}
			else {
				port = smtp.getConfiguration().getPort();
			}
			xoauth2Stage = "client hostname";
			String clientHost = smtp.getConfiguration().getClientHost() == null
				? InetAddress.getLocalHost().getHostName()
				: smtp.getConfiguration().getClientHost();
			
			LoginMethod loginMethod = smtp.getConfiguration().getLoginMethod();
			boolean oauth2 = loginMethod == LoginMethod.XOAUTH2;
			if (oauth2 && (smtp.getConfiguration().getUsername() == null || smtp.getConfiguration().getUsername().trim().isEmpty()
					|| smtp.getConfiguration().getAccessTokenProvider() == null || (!implicitSSL && !startTls))) {
				throw new IllegalArgumentException("XOAUTH2 requires a username, access token provider and TLS");
			}

			// if we want to authenticate, use the appropriate client
			if (smtp.getConfiguration().getUsername() != null) {
				logger.debug("Starting " + (context == null ? "insecure" : "secure") + " connection with authentication");
				AuthenticatingSMTPClient client = context == null 
					? new AuthenticatingSMTPClient() 
					: new AuthenticatingSMTPClient(implicitSSL, context);
				try {
					if (oauth2) {
						client.setEndpointCheckingEnabled(true);
					}
					// set the connection timeout
					if (smtp.getConfiguration().getConnectionTimeout() != null) {
						client.setConnectTimeout(smtp.getConfiguration().getConnectionTimeout());
						// not entirely sure how this differs from the connection timeout...
						client.setDefaultTimeout(smtp.getConfiguration().getConnectionTimeout());
					}
					else {
						// default to 1 minute
						client.setConnectTimeout(60000);
						client.setDefaultTimeout(60000);
					}
					// set charset (if any)
					if (smtp.getConfiguration().getCharset() != null && !oauth2) {
						client.setCharset(Charset.forName(smtp.getConfiguration().getCharset()));
					}
					
					logger.debug("Connecting to: " + smtp.getConfiguration().getHost() + ":" + port);
					xoauth2Stage = implicitSSL ? "connect/TLS" : "connect";
					// connect
					client.connect(smtp.getConfiguration().getHost(), port);
					if (oauth2) {
						if (!SMTPReply.isPositiveCompletion(client.getReplyCode())) {
							throw xoauth2Failure("connect", client.getReplyCode(), null);
						}
					}
					else {
						checkReply(client, "Could not connect to server");
					}
					
					if (smtp.getConfiguration().getSocketTimeout() != null) {
						client.setSoTimeout(smtp.getConfiguration().getSocketTimeout());
					}
					else {
						// also default to 1 minute
						client.setSoTimeout(60000);
					}
				}
				catch (Exception e) {
					if (oauth2) {
						throw e;
					}
					logger.error("Failed to set up connection", e);
					throw new RuntimeException(e);
				}
				boolean failed = false;
				try {
					logger.debug("Sending EHLO");
					// perform an ehlo
					xoauth2Stage = "EHLO";
					int ehloReply = client.ehlo(clientHost);
					if (oauth2 && !SMTPReply.isPositiveCompletion(ehloReply)) {
						throw xoauth2Failure("EHLO", ehloReply, null);
					}
					if (!oauth2) {
						checkReply(client, ehloReply, "Failed the ehlo command");
					}
					
					// we want a secure connection if possible
					if (startTls && (!oauth2 || !implicitSSL)) {
						xoauth2Stage = "STARTTLS";
						if (!client.execTLS()) {
							throw new RuntimeException("Could not start tls");
						}
						// resend the ehlo according to spec: http://www.ietf.org/rfc/rfc3207.txt (section 4.2)
						xoauth2Stage = "EHLO after TLS";
						int tlsEhloReply = client.ehlo(clientHost);
						if (oauth2 && !SMTPReply.isPositiveCompletion(tlsEhloReply)) {
							throw xoauth2Failure("EHLO after TLS", tlsEhloReply, null);
						}
						if (!oauth2) {
							checkReply(client, tlsEhloReply, "Failed the ehlo command");
						}
					}
					LoginMethod method = loginMethod == null ? LoginMethod.CRAM_MD5 : loginMethod;
					if (oauth2) {
						xoauth2Stage = "sender validation";
						from = effectiveSender(from);
						xoauth2Stage = "token-provider";
						// Reuse the caller's execution context; never elevate token retrieval to ROOT.
						String token = invokeAccessTokenProvider(smtp.getConfiguration().getAccessTokenProvider(),
							smtp.getId(), smtp.getConfiguration().getUsername(), from, smtp.getConfiguration().getContext(), executionContext);
						xoauth2Stage = "auth";
						authXoauth2(client, smtp.getConfiguration().getUsername(), token);
					}
					else {
						logger.debug("Logging in using method '" + method + "' and username: " + smtp.getConfiguration().getUsername());
						client.auth(method.getMethod(), smtp.getConfiguration().getUsername(), smtp.getConfiguration().getPassword());
						checkReply(client, "Failed the login");
					}
					
					logger.debug("Setting sender: {}", from);
					// set sender/recipients
					xoauth2Stage = "envelope sender";
					client.setSender(from);
					if (oauth2 && !SMTPReply.isPositiveCompletion(client.getReplyCode())) {
						throw xoauth2Failure(xoauth2Stage, client.getReplyCode(), null);
					}
					if (!oauth2) {
						checkReply(client, "Failed to set sender: " + from);
					}
					xoauth2Stage = "recipients";
					for (String recipient : to) {
						logger.debug("Setting recipient: {}", recipient);
						client.addRecipient(recipient);
						if (oauth2 && !SMTPReply.isPositiveCompletion(client.getReplyCode())) {
							throw xoauth2Failure(xoauth2Stage, client.getReplyCode(), null);
						}
						if (!oauth2) {
							checkReply(client, "Failed to set recipient: " + recipient);
						}
					}
					// it is often interesting to also combine all sent mails in a central mailbox for later complaints
					if (smtp.getConfig().getBcc() != null && !smtp.getConfig().getBcc().isEmpty()) {
						for (String recipient : smtp.getConfig().getBcc()) {
							client.addRecipient(recipient);
							if (oauth2 && !SMTPReply.isPositiveCompletion(client.getReplyCode())) {
								throw xoauth2Failure(xoauth2Stage, client.getReplyCode(), null);
							}
							if (!oauth2) {
								checkReply(client, "Failed to set recipient: " + recipient);
							}
						}
					}
					
					logger.debug("Sending data");
					// let's start writing...
					xoauth2Stage = "DATA command";
					Writer writer = client.sendMessageData();
					if (writer == null) {
						if (oauth2) {
							throw xoauth2Failure(xoauth2Stage, client.getReplyCode(), null);
						}
						checkReply(client, "Could not start DATA");
						// https://commons.apache.org/proper/commons-net/javadocs/api-3.6/org/apache/commons/net/smtp/SMTPClient.html#sendMessageData()
						// Send the SMTP DATA command in preparation to send an email message. This method returns a DotTerminatedMessageWriter instance to which the message can be written. Null is returned if the DATA command fails.
						// not sure if the check reply can actually check why it failed, throw an exception just in case
						throw new RuntimeException("Could not send SMTP DATA command");
					}
					xoauth2Stage = "DATA write (delivery uncertain)";
					MimeFormatter formatter = new MimeFormatter();
					if (quoteBoundary != null) {
						formatter.setQuoteBoundary(quoteBoundary);
					}
					WritableStraightByteToCharContainer output = new WritableStraightByteToCharContainer(IOUtils.wrap(writer));
					formatter.format(part, output);
					output.close();
					xoauth2Stage = "DATA completion (delivery uncertain)";
					if (!client.completePendingCommand()) {
						if (oauth2) {
							throw xoauth2Failure(xoauth2Stage, client.getReplyCode(), null);
						}
						throw new RuntimeException("Could not send the data: " + client.getReplyCode() + " : " + client.getReplyString());
					}
				}
				catch (Exception e) {
					if (!oauth2) {
						logger.error("Could not complete request", e);
					}
					failed = true;
					throw e;
				}
				finally {
					try {
						client.logout();
						client.disconnect();
					}
					catch (Exception e) {
						if (oauth2) {
							if (!failed) {
								xoauth2Stage = "disconnect (delivery uncertain)";
								throw e;
							}
							logger.warn("SMTP XOAUTH2 disconnect failed (category={})", safeXoauth2Category(e));
						}
						else if (!failed) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			else {
				logger.debug("Starting " + (context == null ? "insecure" : "secure") + " connection anonymously");
				SMTPClient client = context == null ? new SMTPClient() : new SMTPSClient(implicitSSL, context);
				try {
					// set the connection timeout
					if (smtp.getConfiguration().getConnectionTimeout() != null) {
						client.setConnectTimeout(smtp.getConfiguration().getConnectionTimeout());
						client.setDefaultTimeout(smtp.getConfiguration().getConnectionTimeout());
					}
					else {
						// default to 1 minute
						client.setConnectTimeout(60000);
						client.setDefaultTimeout(60000);
					}
					// set the charset (if any)
					if (smtp.getConfiguration().getCharset() != null) {
						client.setCharset(Charset.forName(smtp.getConfiguration().getCharset()));
					}
					
					logger.debug("Connecting to {}:{}", smtp.getConfig().getHost(), port);
					// connect
					client.connect(smtp.getConfiguration().getHost(), port);
					checkReply(client, "Could not connect to server");
		
					if (smtp.getConfiguration().getSocketTimeout() != null) {
						client.setSoTimeout(smtp.getConfiguration().getSocketTimeout());
					}
					else {
						// also default to 1 minute
						client.setSoTimeout(60000);
					}
				}
				catch (Exception e) {
					logger.error("Failed to set up connection", e);
					throw new RuntimeException(e);
				}
				boolean failed = false;
				try {
					logger.debug("Sending HELO");
					// send helo
					checkReply(client, client.helo(clientHost), "Failed the helo command");
					
					if (context == null || implicitSSL || ((SMTPSClient) client).execTLS()) {
						// set sender/recipients
						logger.debug("Setting sender: {}", from);
						client.setSender(from);
						checkReply(client, "Failed to set sender: " + from);
						for (String recipient : to) {
							logger.debug("Adding recipient: {}", recipient);
							client.addRecipient(recipient);
							checkReply(client, "Failed to set recipient: " + recipient);
						}
						
						logger.debug("Sending data");
						// let's start writing...
						Writer writer = client.sendMessageData();
						MimeFormatter formatter = new MimeFormatter();
						if (quoteBoundary != null) {
							formatter.setQuoteBoundary(quoteBoundary);
						}
						WritableStraightByteToCharContainer output = new WritableStraightByteToCharContainer(IOUtils.wrap(writer));
						formatter.format(part, output);
						output.close();
						if (!client.completePendingCommand()) {
							throw new RuntimeException("Could not send the data: " + client.getReply() + " : " + client.getReplyString());
						}
					}
					else {
						throw new RuntimeException("Secure connection could not be established");
					}
				}
				catch (Exception e) {
					logger.error("Could not complete request", e);
					failed = true;
					throw e;
				}
				finally {
					try {
						client.logout();
						client.disconnect();
					}
					catch (Exception e) {
						if (!failed) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
		catch (Exception e) {
			if (smtp.getConfiguration().getLoginMethod() == LoginMethod.XOAUTH2) {
				// Never forward provider exceptions, server replies or their causes to notifications.
				e = e instanceof Xoauth2Failure ? e : xoauth2Failure(xoauth2Stage, 0, e);
				logger.error(e.getMessage());
			}
			if (notifyOnFailure == null || notifyOnFailure) {
				// fire a notification
				Notification notification = new Notification();
				notification.setContext(Arrays.asList(smtpClientId));
				// differentiate between client and server errors
				notification.setType("nabu.protocols.smtp.client");
				notification.setProperties(SmtpRequestSummary.build(from, to, part, quoteBoundary));
				notification.setMessage("Failed to send email: " + e.getMessage());
				notification.setDescription(Notification.format(e));
				notification.setSeverity(Severity.ERROR);
				smtp.getRepository().getEventDispatcher().fire(notification, smtp);
			}
			throw e;
		}
	}
	
	public static class SmtpRequestSummary {
		private List<String> to;
		private String from, content;
		
		public static SmtpRequestSummary build(String from, List<String> to, Part part, Boolean quoteBoundary) {
			SmtpRequestSummary summary = new SmtpRequestSummary();
			summary.setTo(to);
			summary.setFrom(from);
			try {
				MimeFormatter formatter = new MimeFormatter();
				if (quoteBoundary != null) {
					formatter.setQuoteBoundary(quoteBoundary);
				}
				formatter.setAllowBinary(false);
				ByteBuffer buffer = IOUtils.newByteBuffer();
				formatter.format(part, buffer);
				summary.setContent(new String(IOUtils.toBytes(buffer), "UTF-8"));
			}
			catch (Exception e) {
				summary.setContent(Notification.format(e));
			}
			return summary;
		}
		public List<String> getTo() {
			return to;
		}
		public void setTo(List<String> to) {
			this.to = to;
		}
		public String getFrom() {
			return from;
		}
		public void setFrom(String from) {
			this.from = from;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
	}

	static String effectiveSender(String from) {
		if (from == null || from.trim().isEmpty()) {
			throw new IllegalArgumentException("XOAUTH2 requires a single valid sender address");
		}
		for (int i = 0; i < from.length(); i++) {
			if (Character.isISOControl(from.charAt(i))) {
				throw new IllegalArgumentException("XOAUTH2 requires a single valid sender address");
			}
		}
		try {
			InternetAddress[] addresses = InternetAddress.parse(from, true);
			if (addresses.length == 1 && !addresses[0].isGroup()) {
				addresses[0].validate();
				String address = addresses[0].getAddress();
				if (address != null && !address.isEmpty() && address.equals(address.trim())) {
					return address;
				}
			}
		}
		catch (AddressException e) {
			LoggerFactory.getLogger(Services.class).debug("Invalid SMTP XOAUTH2 sender ({})", e.getClass().getName());
			throw new IllegalArgumentException("XOAUTH2 requires a single valid sender address");
		}
		throw new IllegalArgumentException("XOAUTH2 requires a single valid sender address");
	}

	static void authXoauth2(AuthenticatingSMTPClient client, String username, String token) throws IOException {
		if (username == null || username.trim().isEmpty() || token == null || token.trim().isEmpty()
				|| !username.equals(username.trim()) || !token.equals(token.trim())) {
			throw new IllegalArgumentException("Invalid XOAUTH2 credentials");
		}
		for (String value : Arrays.asList(username, token)) {
			for (int i = 0; i < value.length(); i++) {
				if (Character.isISOControl(value.charAt(i))) {
					throw new IllegalArgumentException("Invalid XOAUTH2 credentials");
				}
			}
		}
		String payload = "user=" + username + "\u0001auth=Bearer " + token + "\u0001\u0001";
		int reply = client.sendCommand("AUTH XOAUTH2 " + Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));
		if (reply == 334) {
			// Complete the server's error challenge without sending credentials again.
			client.sendCommand("");
		}
		if (reply != 235) {
			throw xoauth2Failure("auth", reply, null);
		}
	}

	private static final class Xoauth2Failure extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		private Xoauth2Failure(String message) {
			super(message);
		}
	}

	static IllegalStateException xoauth2Failure(String stage, int replyCode, Exception cause) {
		String category = cause == null ? "SMTP rejection" : safeXoauth2Category(cause);
		String code = replyCode >= 100 && replyCode <= 599 ? ", reply=" + replyCode : "";
		return new Xoauth2Failure("SMTP XOAUTH2 send failed (stage=" + stage + code + ", category=" + category + ")");
	}

	private static String safeXoauth2Category(Exception e) {
		if (e instanceof java.net.SocketTimeoutException) {
			return "timeout";
		}
		if (e instanceof javax.net.ssl.SSLException) {
			return "TLS error";
		}
		if (e instanceof IOException) {
			return "I/O error";
		}
		if (e instanceof IllegalArgumentException) {
			return "invalid input";
		}
		return "operation error";
	}

	private void checkReply(SMTPClient client, String message) {
		checkReply(client, client.getReplyCode(), message);
	}
	
	private void checkReply(SMTPClient client, int code, String message) {
		if (!SMTPReply.isPositiveCompletion(client.getReplyCode())) {
			throw new RuntimeException("[" + client.getReplyCode() + "] " + client.getReplyString() + ": " + message);
		}
	}
}
