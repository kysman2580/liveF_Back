package org.livef.livef_authservice.auth.model.service;

import java.util.Map;

public interface KakaoService {

	Map<String, Object> getKakaoLoginUrl();
	
	Map<String, Object> getKakaoAcessToken(String code);

}
