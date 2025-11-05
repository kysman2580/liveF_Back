package org.livef.livef_authservice.token.util;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TokenUtil {

	@Value("${jwt.secret}")
	private String secretKey;
	private SecretKey key;

	@PostConstruct
	public void init() {	// 시크릿키 디코딩 및, 서명 키 생성
		byte[] keyArr = Base64.getDecoder().decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyArr);
	}

	public String getAccessToken(Long memberNo, String username, String role) { // ★ role 파라미터 추가
		log.info("토큰 생성 - 사용자명: {}, 역할: {}", username, role); // 로그 수정

		return Jwts.builder()
				.subject(username)
				.claim("memberNo", memberNo)
				.claim("role", role) // ★★★ role 클레임 추가 ★★★
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
				.signWith(key)
				.compact();
	}

	public String getRefreshToken(Long memberNo, String username, String role) { // ★ role 파라미터 추가
		return Jwts.builder()
				.subject(username)
				.claim("memberNo", memberNo)
				.claim("role", role) // ★★★ role 클레임 추가 ★★★
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3))
				.signWith(key)
				.compact();
	}

	public Claims parseJwt(String token) { // JWT 검증 및 페이로드 추출

		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

}
