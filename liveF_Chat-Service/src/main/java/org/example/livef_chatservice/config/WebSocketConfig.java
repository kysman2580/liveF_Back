package org.example.livef_chatservice.config;

import lombok.RequiredArgsConstructor;
import org.example.livef_chatservice.security.JwtHandshakeInterceptor;
import org.example.livef_chatservice.security.StompChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompChannelInterceptor stompChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("=".repeat(80));
        System.out.println("✅ WebSocket 엔드포인트 등록 시작");

        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();

        System.out.println("✅ /ws 엔드포인트 등록 완료 (SockJS 활성화, CORS 설정 추가)");
        System.out.println("=".repeat(80));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");

        System.out.println("✅ 메시지 브로커 설정 완료: /app, /topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
        System.out.println("✅ STOMP 채널 인터셉터 등록 완료");
    }
}
