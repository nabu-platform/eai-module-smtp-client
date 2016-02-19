package nabu.protocols.smtp.client.types;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "smtpClientInformation")
public class SMTPClientInformation {
	private String host, username, clientHost;
	private Integer port, connectionTimeout, socketTimeout;
	private Boolean implicitSSL;
	private String charset;
	private String blacklist;
	
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
	public String getClientHost() {
		return clientHost;
	}
	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
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
	public Boolean getImplicitSSL() {
		return implicitSSL;
	}
	public void setImplicitSSL(Boolean implicitSSL) {
		this.implicitSSL = implicitSSL;
	}
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public String getBlacklist() {
		return blacklist;
	}
	public void setBlacklist(String blacklist) {
		this.blacklist = blacklist;
	}
	
}
