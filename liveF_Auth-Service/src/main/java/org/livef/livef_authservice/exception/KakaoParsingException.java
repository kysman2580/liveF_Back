package org.livef.livef_authservice.exception;

import org.springframework.security.core.AuthenticationException;

public class KakaoParsingException extends AuthenticationException {

	public KakaoParsingException(String message) {
		super(message);
	}
}
