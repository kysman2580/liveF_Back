package org.livef.livef_apigateway.component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


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
                Claims claims = util.parseJwt(rawToken);

                String username = claims.getSubject();
                String role = String.valueOf(claims.get("role")); // null-safe toString
                Number no = (Number) claims.get("memberNo");
                Long memberNo = no == null ? null : no.longValue();

                Collection<? extends GrantedAuthority> authorities =
                        (role == null || role.isBlank())
                                ? Collections.emptyList()
                                : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                ((AbstractAuthenticationToken) auth).setDetails(claims); // 필요 시 다운스트림에서 활용
                return auth;

            } catch (ExpiredJwtException e) {
                throw new BadCredentialsException("토큰이 만료되었습니다", e);
            } catch (JwtException e) {
                throw new BadCredentialsException("유효하지 않은 토큰입니다", e);
            } catch (Exception e) {
                throw new BadCredentialsException("토큰 검증 중 오류", e);
            }
        });
    }
}