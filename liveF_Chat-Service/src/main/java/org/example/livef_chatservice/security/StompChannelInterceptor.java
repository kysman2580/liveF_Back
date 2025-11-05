package org.example.livef_chatservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * STOMP 프레임 인터셉터
 * - CONNECT 프레임에서 WebSocket 세션의 인증 정보를 Principal로 설정
 * - 이후 모든 STOMP 프레임(SEND, SUBSCRIBE 등)에서 이 Principal 사용
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // Spring Security 인터셉터보다 먼저 실행
public class StompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        log.info("📨 STOMP Command: {}", accessor.getCommand());

        // CONNECT 프레임에서만 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("🔐 STOMP CONNECT 프레임 인증 처리 시작");

            // WebSocket 세션 속성에서 인증 정보 가져오기 (JwtHandshakeInterceptor에서 설정한 값)
            String username = (String) accessor.getSessionAttributes().get("username");
            String role = (String) accessor.getSessionAttributes().get("role");

            log.info("👤 Session에서 가져온 정보 - username: {}, role: {}", username, role);

            if (username != null && !username.isBlank()) {
                // 권한 설정
                List<GrantedAuthority> authorities = new ArrayList<>();
                String finalRole = (role != null && !role.isBlank()) ? role : "USER";
                authorities.add(new SimpleGrantedAuthority("ROLE_" + finalRole.toUpperCase()));

                // UserDetails 생성
                UserDetails userDetails = User.withUsername(username)
                        .password("{noop}N/A")
                        .authorities(authorities)
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .build();

                // Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // ⭐️ 핵심: STOMP Principal 설정
                accessor.setUser(authentication);

                log.info("✅ STOMP Principal 설정 완료: username={}, authorities={}",
                        username, authorities);
            } else {
                log.warn("⚠️ STOMP CONNECT: 인증 정보 없음 (username이 null)");
            }
        }

        // 다른 프레임(SEND, SUBSCRIBE 등)은 이미 설정된 Principal 사용
        return message;
    }
}