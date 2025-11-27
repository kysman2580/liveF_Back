package org.livef.livef_authservice.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomAuthenticationException.class)
	public ResponseEntity<?> CustomAuthenticationError(CustomAuthenticationException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
	
	@ExceptionHandler(KakaoParsingException.class)
	public ResponseEntity<?> KakaoParsingError(KakaoParsingException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
	
	@ExceptionHandler(FailedKakaoTokenRequestException.class)
	public ResponseEntity<?> FailedKakaoTokenRequestError(FailedKakaoTokenRequestException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
	
	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<?> InvalidTokenError(InvalidTokenException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
	
	@ExceptionHandler(FailedRequestInfoException.class)
	public ResponseEntity<?> FailedRequestInfoError(FailedRequestInfoException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
	
	@ExceptionHandler(FailedParsingInfoException.class)
	public ResponseEntity<?> FailedParsingInfoError(FailedParsingInfoException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
}
