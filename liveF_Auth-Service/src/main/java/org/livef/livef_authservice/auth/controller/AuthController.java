package org.livef.livef_authservice.auth.controller;

import java.util.Arrays;
import java.util.Map;

import org.livef.livef_authservice.auth.model.dto.MemberDTO;
import org.livef.livef_authservice.auth.model.service.AuthService;
import org.livef.livef_authservice.token.model.service.TokenService;
import org.livef.livef_authservice.util.response.ResponseUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final TokenService tokenService;
	private final ResponseUtil responseUtil;
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody MemberDTO member) {
		log.info("member : {}",member);
		Map<String, Object> data = authService.login(member);
		
	    ResponseCookie access  = (ResponseCookie) data.get("accessCookie");
	    ResponseCookie refresh = (ResponseCookie) data.get("refreshCookie");
	    return ResponseEntity.ok()
	            .header(HttpHeaders.SET_COOKIE, access.toString())
	            .header(HttpHeaders.SET_COOKIE, refresh.toString())
	            .body(responseUtil.getResponseData(data, "로그인 되었습니다", "201"));
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response){
		log.info("Cookies: {}", Arrays.toString(request.getCookies()));
		 String refreshToken = null;
		    if (request.getCookies() != null) {
		        for (Cookie cookie : request.getCookies()) {
		        	log.info("Cookie name={}, value={}", cookie.getName(), cookie.getValue());
		            if ("REFRESH_TOKEN".equals(cookie.getName())) {
		                refreshToken = cookie.getValue();
		                break;
		            }
		        }
		    }
		Map<String, Object> newToken = tokenService.checkRefreshToken(refreshToken);
		ResponseCookie access  = (ResponseCookie) newToken.get("accessCookie");
	    ResponseCookie refresh = (ResponseCookie) newToken.get("refreshCookie");
	    
	    Map<String, Object> data = authService.getMemberInfo(refreshToken);
	    return ResponseEntity.ok()
	            .header(HttpHeaders.SET_COOKIE, access.toString())
	            .header(HttpHeaders.SET_COOKIE, refresh.toString())
	            .body(responseUtil.getResponseData(data,"토큰이 재발급 되었습니다", "201"));
	}
	
	@DeleteMapping("/logout")
	public ResponseEntity<?> logout(@RequestHeader(value="X-User-No", required=false) Long memberNo, Authentication authentication) {
		 log.info("auth type={}, principal={}",
	             authentication == null ? null : authentication.getClass().getSimpleName(),
	             authentication == null ? null : authentication.getPrincipal());
		log.info("{}","들어왔나???");
		Map<String, Object> data = authService.logout(memberNo);
		ResponseCookie access  = (ResponseCookie) data.get("accessCookie");
	    ResponseCookie refresh = (ResponseCookie) data.get("refreshCookie");
	    return ResponseEntity.ok()
	            .header(HttpHeaders.SET_COOKIE, access.toString())
	            .header(HttpHeaders.SET_COOKIE, refresh.toString())
	            .body(responseUtil.getResponseData("로그아웃 되었습니다.", "201"));
	}
}
