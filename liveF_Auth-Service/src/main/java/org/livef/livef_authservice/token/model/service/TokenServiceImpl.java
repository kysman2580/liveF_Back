package org.livef.livef_authservice.token.model.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.livef.livef_authservice.auth.model.service.AuthService;
import org.livef.livef_authservice.auth.model.service.AuthServiceImpl;
import org.livef.livef_authservice.token.model.domain.RefreshToken;
import org.livef.livef_authservice.token.model.repository.RefreshTokenRepository;
import org.livef.livef_authservice.token.util.TokenUtil;
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
	
	@Override
	public Map<String, Object> generateToken(String memberId, Long memberNo) {
		Map<String, Object> tokens = createToken(memberNo,memberId);
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
		
		String username = getUsernameByToken(refreshToken);
		return generateToken(username, memberNo);
	}
	

	private String getUsernameByToken(String refreshToken) {
		Claims claims = tokenUtil.parseJwt(refreshToken);
		return claims.getSubject();
	}

    @Override
    public void deleteRefreshToken(Long memberNo) {
    	refreshTokenRepository.deleteByMemberNo(memberNo);
    }
}