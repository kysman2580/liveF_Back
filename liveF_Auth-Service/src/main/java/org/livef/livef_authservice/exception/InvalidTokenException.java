package org.livef.livef_authservice.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidTokenException extends AuthenticationException {
	public InvalidTokenException(String message) {
		super(message);
	}
}
