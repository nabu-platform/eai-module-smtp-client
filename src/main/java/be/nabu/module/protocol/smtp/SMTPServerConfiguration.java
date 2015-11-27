package be.nabu.module.protocol.smtp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.utils.security.EncryptionXmlAdapter;

@XmlRootElement(name = "smtpServer")
@XmlType(propOrder = { "keystoreId", "implicitSSL", "host", "port", "username", "password", "loginMethod", "clientHost", "connectionTimeout", "socketTimeout", "charset" })
public class SMTPServerConfiguration {
	
	private String keystoreId;
	private String host, username, password;
	private LoginMethod loginMethod;
	private String clientHost;
	private Integer port, connectionTimeout, socketTimeout;
	private Boolean implicitSSL;
	private String charset;
	
	public String getKeystoreId() {
		return keystoreId;
	}
	public void setKeystoreId(String keystoreId) {
		this.keystoreId = keystoreId;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	@XmlJavaTypeAdapter(value=EncryptionXmlAdapter.class)
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public Boolean getImplicitSSL() {
		return implicitSSL;
	}
	public void setImplicitSSL(Boolean implicitSSL) {
		this.implicitSSL = implicitSSL;
	}
	public String getClientHost() {
		return clientHost;
	}
	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}
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
