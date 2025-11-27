package org.livef.livef_authservice.token.model.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 60*60*24*3)
public class RefreshToken {

	@Id
	private String refreshToken;
	
	@Indexed
	private Long memberNo;
	
}
