package be.nabu.module.protocol.smtp;

import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.AuthenticatingSMTPClient.AUTH_METHOD;

public enum LoginMethod {
	
	CRAM_MD5(AuthenticatingSMTPClient.AUTH_METHOD.CRAM_MD5),
	PLAIN(AuthenticatingSMTPClient.AUTH_METHOD.PLAIN),
	LOGIN(AuthenticatingSMTPClient.AUTH_METHOD.LOGIN),
	XOAUTH(AuthenticatingSMTPClient.AUTH_METHOD.XOAUTH)
	;
	
	private AUTH_METHOD method;

	private LoginMethod(AuthenticatingSMTPClient.AUTH_METHOD method) {
		this.method = method;
	}
	public AUTH_METHOD getMethod() {
		return method;
	}
	
}
