package org.livef.livef_apigateway.component;


import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered{

	private final JwtUtil util;
	

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

       /* 
        * 2. header에 토큰이 있는지 / Bearer로 시작하는지 검증
        * 3. 아니라면 그냥 다음필터 진행 하도록 리턴
        * 4. 맞다면 토큰을 파싱해서 payload에서 claims(사용자 정보) 추출
        * 5. 사용자 정보를 추출하여 헤더에 담아서 각 서비스로 가게함.
        */
    	
    	
    	String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    	
    	if (authorization == null || !authorization.startsWith("Bearer ")) {
    		return chain.filter(exchange);
        }
    	String token = authorization.substring(7);

    	try {
             // JWT 검증 및 파싱
            
    	   Claims claims = util.parseJwt(token);
             
    	   Long memberNo = Optional.ofNullable(claims.get("memberNo", Number.class))
                   .map(Number::longValue).orElse(null);
           String username = claims.getSubject();
           String role = (String) claims.get("role");
             
             // 사용자 정보를 헤더에 담기
           exchange = exchange.mutate()
               .request(exchange.getRequest().mutate()
                   .header("X-Username", username)
                   .header("X-User-No", String.valueOf(memberNo))
                   .header("X-User-Role", role)
                   .build())
               .build();
             
            return chain.filter(exchange);
             
    	 } catch (ExpiredJwtException e) {
             log.info("만료된 토큰: {}", e.getMessage());
             return handleUnauthorized(exchange, "만료된 토큰입니다.");
             
         } catch (JwtException e) {
             log.info("유효하지 않은 토큰: {}", e.getMessage());
             return handleUnauthorized(exchange, "유효하지 않은 토큰입니다.");
             
         } catch (Exception e) {
             log.info("JWT 필터 처리 중 오류 발생", e);
             return handleUnauthorized(exchange, "인증 처리 중 오류가 발생했습니다.");
         }
                
    }
    
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = message.getBytes();
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

