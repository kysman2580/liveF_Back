package org.livef.livef_apigateway.security;

import org.livef.livef_apigateway.component.JwtAuthenticationWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain security(ServerHttpSecurity http,
                                           JwtAuthenticationWebFilter jwtFilter) {

        log.info("SecurityConfig 로드 완료");

        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // 401 응답 커스텀 (브라우저 로그인 팝업 방지)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )

                // .addFilterAfter(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 경로별 인증 설정
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // 1. WebSocket 전부 공개 (제일 위에!)
                        .pathMatchers("/ws/**", "/ws/info/**", "/ws", "/ws/").permitAll()

                        // 2. 인증 필요 없는 API들
                        .pathMatchers("/api/auth/login", "/api/auth/refresh",
                                "/api/member/sign-up", "/api/auth/kakao/**",
                                "/api/member/check-id", "/api/member/change-password").permitAll()

                        // 3. 공개 데이터 API
                        .pathMatchers(HttpMethod.GET, "/api/v1/feed/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/fixture/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/team/**").permitAll()

                        // 4. 인증 필요한 API들 (명시적으로)
                        .pathMatchers(HttpMethod.POST, "/api/**").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/api/**").authenticated()
                        .pathMatchers(HttpMethod.PATCH, "/api/**").authenticated()
                        .pathMatchers(HttpMethod.DELETE, "/api/**").authenticated()

                        // 5. 그 외 GET /api/** 도 인증 필요 (선택)
                        .pathMatchers(HttpMethod.GET, "/api/**").authenticated()

                        // 🔥 여기서 .anyExchange().permitAll() → .anyExchange().authenticated() 로 변경!
                        // 또는 아예 제거 (위에서 다 커버됨)
                        .anyExchange().permitAll()
                )
                .build();
    }
}