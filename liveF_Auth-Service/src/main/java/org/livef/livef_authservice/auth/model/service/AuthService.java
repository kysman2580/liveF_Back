package org.livef.livef_authservice.auth.model.service;

import java.util.Map;

import org.livef.livef_authservice.auth.model.dto.MemberDTO;
import org.livef.livef_authservice.auth.model.vo.CustomUserDetails;

public interface AuthService {

	Map<String, Object> login(MemberDTO member);
	
	CustomUserDetails getUserDetails();

	Map<String, Object> logout(Long memberNo);

	Map<String, Object> getMemberInfo(String refreshToken);
}
