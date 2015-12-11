package nabu.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;

import nabu.types.EmailAttachment;

import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SMTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.smtp.EmailType;
import be.nabu.eai.module.smtp.LoginMethod;
import be.nabu.eai.module.smtp.SMTPServerArtifact;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.containers.chars.WritableStraightByteToCharContainer;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.api.Part;
import be.nabu.utils.mime.impl.FormatException;
import be.nabu.utils.mime.impl.MimeFormatter;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeMultiPart;
import be.nabu.utils.security.SSLContextType;

@WebService
public class Smtp {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private ExecutionContext executionContext;
	
	@WebResult(name = "part")
	public Part newEmailPart(@WebParam(name = "from") String from, @WebParam(name = "to") List<String> to, @WebParam(name = "cc") List<String> cc, @WebParam(name = "subject") String subject, @WebParam(name = "content") InputStream content, @WebParam(name = "type") EmailType type, @WebParam(name = "headers") List<Header> headers, @WebParam(name = "attachments") List<EmailAttachment> attachments) {
		if (type == null) {
			type = EmailType.HTML;
		}
		ModifiablePart part = new PlainMimeContentPart(null, IOUtils.wrap(content), 
			new MimeHeader("Content-Type", type.getContentType())
		);
		if (attachments != null && !attachments.isEmpty()) {
			PlainMimeMultiPart multiPart = new PlainMimeMultiPart(null);
			// first we add the content part
			multiPart.addChild(part);
			// add the attachments
			for (EmailAttachment attachment : attachments) {
				PlainMimeContentPart attachmentPart = new PlainMimeContentPart(multiPart, IOUtils.wrap(content), 
					new MimeHeader("Content-Type", attachment.getContentType())
				);
				MimeHeader dispositionHeader = new MimeHeader("Content-Disposition", attachment.getInline() != null && attachment.getInline() ? "inline" : "attachment");
				if (attachment.getHeaders() != null) {
					for (Header header : attachment.getHeaders()) {
						attachmentPart.setHeader(header);
					}
				}
				if (attachment.getName() != null) {
					dispositionHeader.addComment("filename=" + attachment.getName());
				}
				attachmentPart.setHeader(dispositionHeader);
				multiPart.addChild(attachmentPart);
			}
			part = multiPart;
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
		return part;
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
	
	public void send(@WebParam(name = "part") Part part, @WebParam(name = "from") @NotNull String from, @WebParam(name = "to") @NotNull List<String> to, @WebParam(name = "smtpServerId") @NotNull String smtpServerId) throws IOException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, InvalidKeySpecException, FormatException {
		if (part == null) {
			return;
		}
		// get the smtp server
		SMTPServerArtifact smtp = executionContext.getServiceContext().getResolver(SMTPServerArtifact.class).resolve(smtpServerId);
		if (smtp == null) {
			throw new IllegalArgumentException("Could not find the smtp server: " + smtpServerId);
		}
		
		// check if we have a configured keystore
		DefinedKeyStore keystore = smtp.getConfiguration().getKeystore();
		SSLContext context = keystore == null ? null : keystore.getKeyStore().newContext(SSLContextType.TLS);
		// use the default context if you have explicitly set the implicitSSL boolean
		if (smtp.getConfiguration().getImplicitSSL() != null && context == null) {
			context = SSLContext.getDefault();
		}
		
		// check if we want implicit ssl
		boolean implicitSSL = smtp.getConfiguration().getImplicitSSL() != null && smtp.getConfiguration().getImplicitSSL();
		
		// check the port
		int port;
		if (smtp.getConfiguration().getPort() == null) {
			port = implicitSSL ? 465 : 25;
		}
		else {
			port = smtp.getConfiguration().getPort();
		}
		String clientHost = smtp.getConfiguration().getClientHost() == null
			? InetAddress.getLocalHost().getHostName()
			: smtp.getConfiguration().getClientHost();
		
		// if we want to authenticate, use the appropriate client
		if (smtp.getConfiguration().getUsername() != null) {
			logger.debug("Starting " + (context == null ? "insecure" : "secure") + " connection with authentication");
			AuthenticatingSMTPClient client = context == null 
				? new AuthenticatingSMTPClient() 
				: new AuthenticatingSMTPClient(implicitSSL, context);
			// set the connection timeout
			if (smtp.getConfiguration().getConnectionTimeout() != null) {
				client.setConnectTimeout(smtp.getConfiguration().getConnectionTimeout());
			}
			// not entirely sure how this differs from the connection timeout...
			if (smtp.getConfiguration().getConnectionTimeout() != null) {
				client.setDefaultTimeout(smtp.getConfiguration().getConnectionTimeout());
			}
			// set charset (if any)
			if (smtp.getConfiguration().getCharset() != null) {
				client.setCharset(Charset.forName(smtp.getConfiguration().getCharset()));
			}
			
			logger.debug("Connecting to: " + smtp.getConfiguration().getHost() + ":" + port);
			// connect
			client.connect(smtp.getConfiguration().getHost(), port);
			checkReply(client, "Could not connect to server");
			
			if (smtp.getConfiguration().getSocketTimeout() != null) {
				client.setSoTimeout(smtp.getConfiguration().getSocketTimeout());
			}
			try {
				logger.debug("Sending EHLO");
				// perform an ehlo
				checkReply(client, client.ehlo(clientHost), "Failed the ehlo command");
				
				// we want a secure connection if possible
				if (context == null || implicitSSL || client.execTLS()) {
					LoginMethod method = smtp.getConfiguration().getLoginMethod();
					if (method == null) {
						method = LoginMethod.CRAM_MD5;
					}
					
					logger.debug("Logging in using method '" + method + "' and username: " + smtp.getConfiguration().getUsername());
					// authenticate
					client.auth(method.getMethod(), smtp.getConfiguration().getUsername(), smtp.getConfiguration().getPassword());
					checkReply(client, "Failed the login");
					
					logger.debug("Setting sender/receiver");
					// set sender/recipients
					client.setSender(from);
					checkReply(client, "Failed to set sender: " + from);
					for (String recipient : to) {
						client.addRecipient(recipient);
						checkReply(client, "Failed to set recipient: " + recipient);
					}
					
					logger.debug("Sending data");
					// let's start writing...
					Writer writer = client.sendMessageData();
					MimeFormatter formatter = new MimeFormatter();
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
			catch (RuntimeException e) {
				logger.error("Could not complete request", e);
				throw e;
			}
			finally {
				client.logout();
				client.disconnect();
			}
		}
		else {
			SMTPClient client = context == null ? new SMTPClient() : new SMTPSClient(implicitSSL, context);
			// set the connection timeout
			if (smtp.getConfiguration().getConnectionTimeout() != null) {
				client.setConnectTimeout(smtp.getConfiguration().getConnectionTimeout());
			}
			// again, not sure what this is
			if (smtp.getConfiguration().getConnectionTimeout() != null) {
				client.setDefaultTimeout(smtp.getConfiguration().getConnectionTimeout());
			}
			// set the charset (if any)
			if (smtp.getConfiguration().getCharset() != null) {
				client.setCharset(Charset.forName(smtp.getConfiguration().getCharset()));
			}
				
			// connect
			client.connect(smtp.getConfiguration().getHost(), port);
			checkReply(client, "Could not connect to server");

			if (smtp.getConfiguration().getSocketTimeout() != null) {
				client.setSoTimeout(smtp.getConfiguration().getSocketTimeout());
			}
			try {
				// send helo
				checkReply(client, client.helo(clientHost), "Failed the helo command");
				
				if (context == null || implicitSSL || ((SMTPSClient) client).execTLS()) {
					// set sender/recipients
					client.setSender(from);
					checkReply(client, "Failed to set sender: " + from);
					for (String recipient : to) {
						client.addRecipient(recipient);
						checkReply(client, "Failed to set recipient: " + recipient);
					}
					
					// let's start writing...
					Writer writer = client.sendMessageData();
					MimeFormatter formatter = new MimeFormatter();
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
			catch (RuntimeException e) {
				logger.error("Could not complete request", e);
				throw e;
			}
			finally {
				client.logout();
				client.disconnect();
			}
		}
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
