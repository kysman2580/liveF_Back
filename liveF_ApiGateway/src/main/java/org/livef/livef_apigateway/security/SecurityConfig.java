package org.livef.livef_apigateway.security;

import org.livef.livef_apigateway.component.JwtAuthenticationWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain security(ServerHttpSecurity http,
                                           JwtAuthenticationWebFilter jwtFilter) {

        log.info("SecurityConfig 로드 완료");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // ✅ Gateway의 application.yml에 있는 globalcors 설정을 사용함
                .cors(Customizer.withDefaults())

                // 401 응답 커스텀 (브라우저 로그인 팝업 방지)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )

                // JWT 필터 추가
                .addFilterAfter(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 경로별 인증 설정
                .authorizeExchange(authorize -> authorize
                        // CORS를 위한 Preflight(OPTIONS) 요청은 항상 허용
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // 1. WebSocket 관련 경로 허용
                        .pathMatchers("/ws/**", "/ws/info/**", "/ws", "/ws/").permitAll()

                        // 2. 인증이 필요 없는 API들
                        .pathMatchers("/api/auth/login", "/api/auth/refresh",
                                "/api/member/sign-up", "/api/auth/kakao/**",
                                "/api/member/check-id", "/api/member/change-password").permitAll()

                        // 3. 공개 데이터 조회 API (GET)
                        .pathMatchers(HttpMethod.GET, "/api/v1/feed/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/fixture/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/team/**").permitAll()

                        // 4. 그 외 모든 /api/** 경로는 인증 필요
                        .pathMatchers("/api/**").authenticated()

                        // 5. 나머지는 모두 허용 (배포 환경의 정적 자원 등 대처)
                        .anyExchange().permitAll()
                )
                .build();
    }
}
