package com.livef.livef_memberservice.exception;

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

	@ExceptionHandler(DuplicateMemberIdException.class)
	public ResponseEntity<?> DuplicateMemberIdError(DuplicateMemberIdException e) {
		Map<String, String> error = new HashMap();
		error.put("message", e.getMessage());
		return ResponseEntity.badRequest().body(error);
	}
}
