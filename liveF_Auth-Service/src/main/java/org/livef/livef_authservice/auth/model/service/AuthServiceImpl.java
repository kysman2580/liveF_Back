package org.livef.livef_authservice.auth.model.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.livef.livef_authservice.auth.model.dto.MemberDTO;
import org.livef.livef_authservice.auth.model.entity.MemberEntity;
import org.livef.livef_authservice.auth.model.repository.MemberRepository;
import org.livef.livef_authservice.auth.model.vo.CustomUserDetails;
import org.livef.livef_authservice.exception.CustomAuthenticationException;
import org.livef.livef_authservice.token.model.service.TokenService;
import org.livef.livef_authservice.token.util.TokenUtil;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

	private final AuthenticationManager authenticationManager;
	private Authentication authentication;
	private final TokenService tokenService;
	private final TokenUtil tokenUtil;
	private final MemberRepository memberRepository;

	public Map<String, Object> login(MemberDTO member) {

		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(member.getMemberId(), member.getMemberPw())
			);
		} catch (AuthenticationException e) {
			throw new CustomAuthenticationException("아이디 또는 비밀번호를 잘못 입력하셨습니다.");
		}

		CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
		log.info("인증에 성공한 사용자의 정보 : {}", user);

		if ("N".equals(user.getIsActive())) {
			throw new CustomAuthenticationException("계정이 정지되었습니다.");
		}

		if ("R".equals(user.getIsActive())) {
			throw new CustomAuthenticationException("계정이 비활성화되었습니다.");
		}

		String roleName = user.getAuthorities().stream()
				.findFirst()
				.map(authority -> authority.getAuthority())
				.map(authority -> authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority)
				.orElse("GUEST");

		Map<String, Object> token = tokenService.generateToken(user.getUsername(), user.getMemberNo());
		Map<String, Object> loginResponse = new HashMap<>();
		loginResponse.put("memberId", user.getUsername());
		loginResponse.put("memberNo", user.getMemberNo());
		loginResponse.put("role", roleName);
		loginResponse.put("memberNickname", user.getUsername());
		String accessToken  = (String) token.get("accessToken");
		String refreshToken = (String) token.get("refreshToken");

		// ⭐⭐⭐ 쿠키 생성
		loginResponse.put("accessCookie",  buildCookie("ACCESS_TOKEN",  accessToken,  15 * 60));
		loginResponse.put("refreshCookie", buildCookie("REFRESH_TOKEN", refreshToken, 7 * 24 * 60 * 60));

		log.info("✅ 로그인 성공 - 쿠키 생성 완료: username={}", user.getUsername());

		return loginResponse;
	}

	@Override
	public CustomUserDetails getUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;

		Object p = auth.getPrincipal();
		if (p instanceof CustomUserDetails cud) return cud;
		return null;
	}

	private ResponseCookie buildCookie(String name, String token, int maxAgeSeconds) {
		ResponseCookie cookie = ResponseCookie.from(name, token)
				.path("/")
				.domain("livef.store")    
				.maxAge(maxAgeSeconds)
				.httpOnly(true)           // 보안 유지
				.secure(true)            // 로컬 개발 환경
				.sameSite("None")          // CSRF 보호
				.build();

		log.info("🍪 쿠키 생성: name={}, domain=localhost, httpOnly=true", name);
		return cookie;
	}

	@Override
	public Map<String, Object> logout(Long memberNo) {
		tokenService.deleteRefreshToken(memberNo);
		Map<String, Object> data = new HashMap<>();
		data.put("accessCookie",  buildCookie("ACCESS_TOKEN",  "", 0));
		data.put("refreshCookie", buildCookie("REFRESH_TOKEN", "", 0));
		return data;
	}

	@Override
	public Map<String, Object> getMemberInfo(String refreshToken) {
		String username = getUsernameByToken(refreshToken);
		MemberEntity memberInfo = memberRepository.findByMemberId(username);
		Map<String, Object> loginResponse = new HashMap<>();
		loginResponse.put("memberInfo",  memberInfo);

		return loginResponse;
	}

	private String getUsernameByToken(String refreshToken) {
		Claims claims = tokenUtil.parseJwt(refreshToken);
		return claims.getSubject();
	}
}