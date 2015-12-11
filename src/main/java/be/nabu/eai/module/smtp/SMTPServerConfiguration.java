package be.nabu.eai.module.smtp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.repository.artifacts.keystore.DefinedKeyStore;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.utils.security.EncryptionXmlAdapter;

@XmlRootElement(name = "smtpServer")
@XmlType(propOrder = { "keystore", "implicitSSL", "host", "port", "username", "password", "loginMethod", "clientHost", "connectionTimeout", "socketTimeout", "charset" })
public class SMTPServerConfiguration {
	
	private DefinedKeyStore keystore;
	private String host, username, password;
	private LoginMethod loginMethod;
	private String clientHost;
	private Integer port, connectionTimeout, socketTimeout;
	private Boolean implicitSSL;
	private String charset;
	
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public DefinedKeyStore getKeystore() {
		return keystore;
	}
	public void setKeystore(DefinedKeyStore keystore) {
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
	
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}
	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}
	
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	
}
