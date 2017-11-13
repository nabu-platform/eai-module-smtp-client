package be.nabu.eai.module.smtp;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.security.EncryptionXmlAdapter;

@XmlRootElement(name = "smtpServer")
@XmlType(propOrder = { "keystore", "implicitSSL", "startTls", "host", "port", "username", "password", "loginMethod", "clientHost", "connectionTimeout", "socketTimeout", "charset", "blacklist", "bcc", "overrideTo", "subjectTemplate" })
public class SMTPClientConfiguration {
	
	private KeyStoreArtifact keystore;
	private String host, username, password;
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

}
