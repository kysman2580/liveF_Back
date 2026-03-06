package org.livef.livef_dataservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter authFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable) // ✅ 게이트웨이와 충돌 방지를 위해 잘 끄셨습니다!
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        // A. OPTIONS 요청(CORS Preflight)은 항상 허용
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // B. 웹소켓 및 액추에이터 허용
                        .pathMatchers("/ws/**", "/app/**", "/topic/**", "/actuator/**").permitAll()

                        // C. 공개 데이터 API (Feed, Team, Fixture)
                        .pathMatchers(HttpMethod.GET, "/api/v1/feed/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/team/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/fixture/**").permitAll()

                        // D. 인증이 필요한 특정 API들
                        .pathMatchers(HttpMethod.POST,
                                "/api/test",
                                "/api/member/mypage-info",
                                "/api/auth/password-confirm",
                                "/api/reviews").authenticated()

                        // E. 나머지 모든 /api/** 요청은 인증 필요
                        .pathMatchers("/api/**").authenticated()

                        // F. 그 외 나머지는 모두 허용 (정적 리소스 등)
                        .anyExchange().permitAll()
                )

                // Gateway에서 전달한 헤더 토큰 검증 필터
                .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
