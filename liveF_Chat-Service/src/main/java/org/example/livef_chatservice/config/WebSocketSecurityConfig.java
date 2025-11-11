package org.example.livef_chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

/**
 * 🔥 핵심 변경: @EnableWebSocketSecurity 제거!
 * 이 어노테이션이 자동으로 XorCsrfChannelInterceptor를 등록하여 CSRF 검증을 강제합니다.
 * WebSocket은 자체 인터셉터(StompChannelInterceptor)로 인증을 처리하므로 불필요합니다.
 */
@Configuration
@EnableWebSecurity
public class WebSocketSecurityConfig {

    /**
     * HTTP 레벨 보안 설정
     * WebSocket 핸드셰이크 경로에 대한 보안을 설정합니다
     */
    @Bean
    @Order(1)
    public SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // WebSocket 경로만 처리
                .securityMatcher("/ws/**")

                // CSRF 완전 비활성화
                .csrf(csrf -> csrf.disable())

                // 모든 WebSocket 요청 허용
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}