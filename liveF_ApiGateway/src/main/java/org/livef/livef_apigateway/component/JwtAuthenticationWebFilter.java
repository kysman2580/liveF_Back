package org.livef.livef_apigateway.component;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {

    private final JwtServerAuthenticationConverter converter;

    public JwtAuthenticationWebFilter(JwtReactiveAuthenticationManager authenticationManager,
                                      JwtServerAuthenticationConverter converter) {
        super(authenticationManager);
        this.converter = converter;
        setServerAuthenticationConverter(converter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ✅ WebSocket 요청: JWT 검증은 건너뛰되, 헤더는 추가
        if (path.startsWith("/ws")) {
            return converter.convert(exchange)
                    .flatMap(authentication -> {
                        // JWT에서 사용자 정보 추출
                        String username = authentication.getName();
                        Object userNo = authentication.getCredentials(); // 또는 적절한 필드
                        Object role = authentication.getAuthorities().stream()
                                .findFirst()
                                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                                .orElse("USER");

                        // 헤더 추가
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-Username", username)
                                .header("X-User-No", String.valueOf(userNo))
                                .header("X-User-Role", String.valueOf(role))
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .switchIfEmpty(chain.filter(exchange)); // JWT 없으면 그냥 통과
        }

        // 일반 HTTP 요청: 기존 인증 로직
        return super.filter(exchange, chain)
                .onErrorResume(error -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }
}