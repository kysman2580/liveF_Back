package org.livef.livef_apigateway.component;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {
    
    public JwtAuthenticationWebFilter(JwtReactiveAuthenticationManager authenticationManager,
                                      JwtServerAuthenticationConverter converter) {
        super(authenticationManager);
        setServerAuthenticationConverter(converter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ⭐ WebSocket 경로는 이 필터를 건너뛰기
        if (path.startsWith("/ws")) {
            return chain.filter(exchange);
        }

        return super.filter(exchange, chain);
    }
}