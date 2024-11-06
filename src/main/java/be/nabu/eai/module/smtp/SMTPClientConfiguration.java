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

package be.nabu.eai.module.smtp;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.security.EncryptionXmlAdapter;

@XmlRootElement(name = "smtpServer")
@XmlType(propOrder = { "keystore", "implicitSSL", "startTls", "host", "port", "username", "password", "from", "loginMethod", "clientHost", "connectionTimeout", "socketTimeout", "charset", "blacklist", "bcc", "overrideTo", "overrideToInMime", "subjectTemplate" })
public class SMTPClientConfiguration {
	
	private KeyStoreArtifact keystore;
	private String host, username, password;
	private String from;
	private LoginMethod loginMethod;
	private String clientHost;
	private Integer port, connectionTimeout, socketTimeout;
	private Boolean implicitSSL;
	private Boolean startTls;
	private String charset;
	private String blacklist;
	private List<String> bcc;
	private List<String> overrideTo;
	private String subjectTemplate;
	private Boolean overrideToInMime;
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeystore() {
		return keystore;
	}
	public void setKeystore(KeyStoreArtifact keystore) {
		this.keystore = keystore;
	}
	
	@EnvironmentSpecific
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@EnvironmentSpecific
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value=EncryptionXmlAdapter.class)
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@EnvironmentSpecific
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	@EnvironmentSpecific
	public Boolean getImplicitSSL() {
		return implicitSSL;
	}
	public void setImplicitSSL(Boolean implicitSSL) {
		this.implicitSSL = implicitSSL;
	}
	
	@EnvironmentSpecific
	public String getClientHost() {
		return clientHost;
	}
	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}
	
	@EnvironmentSpecific
	public LoginMethod getLoginMethod() {
		return loginMethod;
	}
	public void setLoginMethod(LoginMethod loginMethod) {
		this.loginMethod = loginMethod;
	}
	
	@EnvironmentSpecific
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	
	@EnvironmentSpecific
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	@EnvironmentSpecific
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	
	@EnvironmentSpecific
	public String getBlacklist() {
		return blacklist;
	}
	public void setBlacklist(String blacklist) {
		this.blacklist = blacklist;
	}
	
	@EnvironmentSpecific
	public Boolean getStartTls() {
		return startTls;
	}
	public void setStartTls(Boolean startTls) {
		this.startTls = startTls;
	}
	
	@EnvironmentSpecific
	public List<String> getBcc() {
		return bcc;
	}
	public void setBcc(List<String> bcc) {
		this.bcc = bcc;
	}
	
	@EnvironmentSpecific
	public List<String> getOverrideTo() {
		return overrideTo;
	}
	public void setOverrideTo(List<String> overrideTo) {
		this.overrideTo = overrideTo;
	}
	
	@EnvironmentSpecific
	public String getSubjectTemplate() {
		return subjectTemplate;
	}
	public void setSubjectTemplate(String subjectTemplate) {
		this.subjectTemplate = subjectTemplate;
	}
	
	@Advanced
	@Comment(title = "Some smtp servers (most notably outlook.office365.com) override the SMTP level RCPT TO with the To header from the mime, if you enable this, the To in the mime will also be overridden. To err on the safe side, this is default true. Note that gmail sends the mail correctly so you can disable this for gmail.")
	public Boolean getOverrideToInMime() {
		return overrideToInMime;
	}
	public void setOverrideToInMime(Boolean overrideToInMime) {
		this.overrideToInMime = overrideToInMime;
	}
	
	@EnvironmentSpecific
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}

}
