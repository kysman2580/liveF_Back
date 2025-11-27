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

        // 일반 HTTP 요청: 기존 인증 로직
        return super.filter(exchange, chain)
                .onErrorResume(error -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }
}