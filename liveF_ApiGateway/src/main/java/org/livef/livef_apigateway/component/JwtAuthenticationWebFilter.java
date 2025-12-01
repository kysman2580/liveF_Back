package org.livef.livef_apigateway.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationWebFilter extends AuthenticationWebFilter {

    private final JwtServerAuthenticationConverter converter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * SecurityConfig에서 permitAll()로 설정된 모든 경로를 여기에 명시하여
     * 인증 필터가 JWT 검증을 건너뛰도록 합니다.
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/feed/**",
            "/ws/**",
            "/ws/info/**",
            "/ws/info",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/member/sign-up",
            "/api/auth/kakao/**",
            "/api/member/check-id",
            "/api/member/change-password"
    );

    public JwtAuthenticationWebFilter(JwtReactiveAuthenticationManager authenticationManager,
                                      JwtServerAuthenticationConverter converter) {
        super(authenticationManager);
        this.converter = converter;
        setServerAuthenticationConverter(converter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. PUBLIC_PATHS에 해당하거나 OPTIONS 메서드 요청이면 인증 필터(super.filter)를 건너뜁니다.
        if (isPublicPath(path) || exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            log.debug("PUBLIC/OPTIONS Path: Bypassing JWT filter for {}", path);
            return chain.filter(exchange);
        }

        // 2. 나머지는 인증 필터를 실행하고, 실패 시 401 처리 (원래 로직)
        log.debug("PROTECTED PATH: Proceeding with JWT authentication for {}", path);
        return super.filter(exchange, chain)
                .onErrorResume(error -> {
                    log.warn("Authentication Failed for {}: {}", path, error.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}