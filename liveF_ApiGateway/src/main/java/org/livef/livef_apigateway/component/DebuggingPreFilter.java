package org.livef.livef_apigateway.component;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DebuggingPreFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("--- [DEBUG: OUTGOING REQUEST] ---");
        exchange.getRequest().getHeaders().forEach((name, values) -> {
            // 쿠키 헤더와 주입된 X-User-No 헤더 확인
            if (name.equalsIgnoreCase("Cookie") || name.equalsIgnoreCase("X-User-No")) {
                log.info("  >> Header: {} = {}", name, values);
            }
        });
        log.info("----------------------------------");
        return chain.filter(exchange);
    }
}