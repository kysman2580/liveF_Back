package com.livef.livef_memberservice.exception;

import org.springframework.security.core.AuthenticationException;

public class DuplicateMemberIdException extends AuthenticationException {

	public DuplicateMemberIdException(String message) {
		super(message);
	}
}
