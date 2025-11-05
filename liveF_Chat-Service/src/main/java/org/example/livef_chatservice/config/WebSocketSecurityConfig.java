package org.example.livef_chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import static org.springframework.messaging.simp.SimpMessageType.CONNECT;
import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT;
import static org.springframework.messaging.simp.SimpMessageType.UNSUBSCRIBE;
import static org.springframework.messaging.simp.SimpMessageType.SUBSCRIBE;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder builder) {

        return builder
                // ⭐ 핵심 수정: CONNECT, DISCONNECT, UNSUBSCRIBE는 Handshake 및 연결 관리 목적으로 permitAll() 허용
                // JwtHandshakeInterceptor가 인증 정보를 세션에 넣을 시간을 줍니다.
                .simpTypeMatchers(CONNECT, DISCONNECT, UNSUBSCRIBE).permitAll()

                // SUBSCRIBE 메시지는 인증된 사용자만 허용
                .simpTypeMatchers(SUBSCRIBE).authenticated()

                // /app으로 전송되는 메시지 (실제 채팅 메시지)는 인증된 사용자만 허용
                .simpDestMatchers("/app/**").authenticated()
                .simpDestMatchers("/topic/**").authenticated()

                // 그 외 모든 메시지는 인증된 사용자에게만 허용
                .anyMessage().authenticated()
                .build();
    }
}