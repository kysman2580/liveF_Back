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

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return Mono.empty();
        }

        //  1) 요청 경로 + RAW Cookie 헤더 찍기
        String path = request.getURI().getPath();
        String rawCookie = request.getHeaders().getFirst(HttpHeaders.COOKIE);
        log.info(">>> PATH        = {}", path);
        log.info(">>> RAW COOKIE  = {}", rawCookie);

        //  2) WebFlux가 파싱한 쿠키 맵에서 ACCESS_TOKEN 꺼내보기
        var cookies = request.getCookies().get(ACCESS_TOKEN_COOKIE);
        log.info(">>> PARSED ACCESS_TOKEN COOKIE = {}", cookies);

        if (cookies != null && !cookies.isEmpty()) {
            String token = cookies.get(0).getValue();
            log.info(">>> ACCESS_TOKEN VALUE = {}", token);

            if (token != null && !token.isBlank()) {
                return Mono.just(new BearerTokenAuthenticationToken(token));
            }
        }

        // 3) 토큰이 전혀 없으면 인증 시도 안 함
        log.info(">>> NO ACCESS_TOKEN FOUND, SKIP AUTH");
        return Mono.empty();
    }
}