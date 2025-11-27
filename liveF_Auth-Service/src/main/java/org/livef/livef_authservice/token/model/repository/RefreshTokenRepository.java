package org.livef.livef_authservice.token.model.repository;


import org.livef.livef_authservice.token.model.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
	
	void deleteByMemberNo(Long memberNo);
}