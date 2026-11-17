package be.nabu.eai.module.smtp.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

public interface SMTPAccessTokenProvider {
	@WebResult(name = "accessToken")
	String getAccessToken(@WebParam(name = "smtpClientId") String smtpClientId,
		@WebParam(name = "username") String username,
		@WebParam(name = "from") String from,
		@WebParam(name = "context") String context);
}
