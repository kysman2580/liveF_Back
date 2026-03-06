package org.livef.livef_authservice.exception;

import org.springframework.security.core.AuthenticationException;

public class FailedRequestInfoException extends AuthenticationException {

	public FailedRequestInfoException(String msg) {
		super(msg);
	}

}
