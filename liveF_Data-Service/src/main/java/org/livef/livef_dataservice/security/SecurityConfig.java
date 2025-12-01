package org.livef.livef_dataservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.server.SecurityWebFilterChain;

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
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .authorizeExchange(exchanges -> exchanges

                        // A. 웹소켓은 인증 제외
                        .pathMatchers("/ws/**",
                                "/app/**",
                                "/topic/**").permitAll()

                        // B. 공개 GET API (feed, team, fixture)
                        .pathMatchers(HttpMethod.GET, "/api/v1/feed/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/team/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/fixture/**").permitAll()

                        // C. 인증 필요한 POST API
                        .pathMatchers(HttpMethod.POST,
                                "/api/test",
                                "/api/member/mypage-info",
                                "/api/auth/password-confirm",
                                "/api/reviews").authenticated()

                        // D. 나머지 변경 요청은 인증 필요
                        .pathMatchers(HttpMethod.DELETE).authenticated()
                        .pathMatchers(HttpMethod.PUT).authenticated()
                        .pathMatchers(HttpMethod.PATCH).authenticated()

                        // E. 그 외 GET 요청은 인증 필요
                        .pathMatchers(HttpMethod.GET).authenticated()

                        // F. 그 외 POST 요청은 인증 필요
                        .pathMatchers(HttpMethod.POST).authenticated()

                        // G. 나머지 모든 요청은 인증 필요
                        .anyExchange().authenticated()
                )

                // Gateway에서 전달한 토큰 검증 필터
                .addFilterAt(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
