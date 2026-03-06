package org.livef.livef_authservice.exception;

import org.springframework.security.core.AuthenticationException;

public class FailedParsingInfoException extends AuthenticationException {

	public FailedParsingInfoException(String msg) {
		super(msg);
		// TODO Auto-generated constructor stub
	}

}
