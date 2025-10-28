package org.livef.livef_apigateway.component;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; 


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    
	private final JwtUtil util;
    
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
    	
    	// Converter에서 만든 토큰을 받음
        BearerTokenAuthenticationToken token = (BearerTokenAuthenticationToken) authentication;
        
        return Mono.fromCallable(() -> {
            try {
            	String rawToken = token.getToken();
            	// 파싱한 후 토큰을 꺼내 claims에 담음
            	Claims claims = util.parseJwt(rawToken);
                
                String username = claims.getSubject();
                String role = (String) claims.get("role");
                Long memberNo = (Long) claims.get("memberNo");
                
                List<GrantedAuthority> authorities = Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_" + role)
                );
                
                Authentication auth = 
                    new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                    );
                
                // Claims를 추가 정보로 저장 (나중에 헤더에 넣을 용도)
                ((AbstractAuthenticationToken) auth).setDetails(claims);
                
                return auth;
                
            } catch (ExpiredJwtException e) {
                throw new BadCredentialsException("토큰이 만료되었습니다", e);
            } catch (JwtException e) {
                throw new BadCredentialsException("유효하지 않은 토큰입니다", e);
            }
        }).onErrorMap(e -> {
            if (e instanceof BadCredentialsException) {
                return e;
            }
            return new BadCredentialsException("토큰 검증 중 오류", e);
        });
    }
}