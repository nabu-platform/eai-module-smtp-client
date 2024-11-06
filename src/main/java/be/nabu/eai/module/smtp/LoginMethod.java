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
