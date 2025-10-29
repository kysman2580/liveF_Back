package org.livef.livef_apigateway.component;

import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {
    
	  private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
	
	  public Mono<Authentication> convert(ServerWebExchange exchange) {
	        ServerHttpRequest request = exchange.getRequest();

	        if (request.getMethod() == HttpMethod.OPTIONS) {
	            return Mono.empty();
	        }

	        var cookies = request.getCookies().get(ACCESS_TOKEN_COOKIE);
	        log.info("cookie : {}",cookies);
	        if (cookies != null && !cookies.isEmpty()) {
	            String token = cookies.get(0).getValue();
	            log.info("token : {}",token);
	            if (token != null && !token.isBlank()) {
	                return Mono.just(new BearerTokenAuthenticationToken(token));
	            }
	        }

	        // 3) 토큰이 전혀 없으면 인증 시도 안 함
	        return Mono.empty();
	    }
}
