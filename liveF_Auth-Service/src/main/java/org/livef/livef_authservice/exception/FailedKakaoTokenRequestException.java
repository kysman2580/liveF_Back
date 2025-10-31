package org.livef.livef_authservice.exception;

import org.springframework.security.core.AuthenticationException;

public class FailedKakaoTokenRequestException extends AuthenticationException {
	public FailedKakaoTokenRequestException(String message) {
		super(message);
	}
}