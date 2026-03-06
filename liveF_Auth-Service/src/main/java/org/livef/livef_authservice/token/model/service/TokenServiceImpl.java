package org.livef.livef_authservice.token.model.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.livef.livef_authservice.auth.model.service.AuthService;
import org.livef.livef_authservice.auth.model.service.AuthServiceImpl;
import org.livef.livef_authservice.token.model.domain.RefreshToken;
import org.livef.livef_authservice.token.model.repository.RefreshTokenRepository;
import org.livef.livef_authservice.token.util.TokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenServiceImpl implements TokenService {

	private final TokenUtil tokenUtil;
	private final RefreshTokenRepository refreshTokenRepository;

	@Value("${app.cookie.domain:}")
	private String cookieDomain;
	@Value("${app.cookie.secure:true}")
	private boolean cookieSecure;

	@Override
	public Map<String, Object> generateToken(String memberId, Long memberNo) {
		Map<String, Object> tokens = createToken(memberNo, memberId);
		saveToken((String) tokens.get("refreshToken"), memberNo);
		return tokens;
	}

	private void saveToken(String refreshToken, Long memberNo) {
		refreshTokenRepository.save(new RefreshToken(refreshToken,memberNo));
	}

	private Map<String, Object> createToken(Long memberNo, String username) {
		String accessToken = tokenUtil.getAccessToken(memberNo, username);
		String refreshToken = tokenUtil.getRefreshToken(memberNo, username);

		Map<String, Object> tokens = new HashMap();
		tokens.put("accessToken", accessToken);
		tokens.put("refreshToken", refreshToken);
		return tokens;
	}

	@Override
	public Map<String, Object> checkRefreshToken(String token) {
		RefreshToken rt = refreshTokenRepository.findById(token)
				.orElseThrow(() -> new RuntimeException("토큰이 없습니다."));
		String refreshToken = rt.getRefreshToken();
		Long memberNo = rt.getMemberNo();

		if (refreshToken == null){
			log.info("토큰이 없습니다잉.");
			throw new RuntimeException("유효하지 않은 토큰입니다.");
		}

		Claims claims = tokenUtil.parseJwt(refreshToken);
		String role = claims.get("role", String.class);
		String username = getUsernameByToken(refreshToken);
		Map<String, Object> tokens =  generateToken(username, memberNo);
		Map<String, Object> Response = new HashMap<>();
		Response.put("accessCookie",  buildCookie("ACCESS_TOKEN",  (String)tokens.get("accessToken"),  15 * 60));
		Response.put("refreshCookie", buildCookie("REFRESH_TOKEN", (String)tokens.get("refreshToken"), 7 * 24 * 60 * 60));
		return Response;
	}

	private ResponseCookie buildCookie(String name, String token, int maxAgeSeconds) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, token)
				.path("/")
				.maxAge(maxAgeSeconds)
				.httpOnly(true)
				.secure(cookieSecure)
				.sameSite(cookieSecure ? "None" : "Lax");
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}
		return builder.build();
	}

	private String getUsernameByToken(String refreshToken) {
		Claims claims = tokenUtil.parseJwt(refreshToken);
		return claims.getSubject();
	}

	@Override
	public void deleteRefreshToken(Long memberNo) {
		refreshTokenRepository.deleteByMemberNo(memberNo);
	}

	public boolean validateAccessToken(String accessToken) {
		try {
			tokenUtil.parseJwt(accessToken);
			return true;
		} catch (Exception e) {
			log.warn("토큰 검증 실패: {}", e.getMessage());
			return false;
		}
	}

	public Long getMemberNoFromAccessToken(String accessToken) {
		Claims claims = tokenUtil.parseJwt(accessToken);
		return claims.get("memberNo", Long.class);
	}
}