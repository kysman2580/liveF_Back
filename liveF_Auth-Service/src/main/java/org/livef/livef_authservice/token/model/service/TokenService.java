package org.livef.livef_authservice.token.model.service;

import java.util.Map;


public interface TokenService {

	/*
	 * 1. access,refresh토큰 생성해서 DTO에 담아 반환하는 메서드
	 * 2. 리프레시 토큰 redis에 저장하는 메서드
	 * 3. 매개변수로 받은 refresh토큰으로 accessToken재발급 해주는 메서드
	 * 4. refresh토큰 redis에서 삭제하는 메서드
	 */
	
	Map<String, Object> generateToken(String memberId, Long memberNo);
	
	Map<String, Object> checkRefreshToken(String token);
	
	void deleteRefreshToken(Long memberNo);
	
	
}
