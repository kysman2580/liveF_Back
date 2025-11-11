// StompChannelInterceptor.java (수정본 - 연결 안정화)
package org.example.livef_chatservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        // Accessor가 null이거나 Command가 null인 경우 (Heartbeat, Polling)
        if (accessor == null || accessor.getCommand() == null) {
            log.trace("Skip: accessor or command is null (heartbeat or polling)");
            return message;
        }

        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        log.info("📨 [STOMP] Command: {}, SessionId: {}", command, sessionId);

        try {
            switch (command) {
                case CONNECT:
                    handleConnect(accessor);
                    break;

                case SUBSCRIBE:
                case SEND:
                case MESSAGE:
                    restoreAuthentication(accessor);
                    break;

                case DISCONNECT:
                    log.info("🔌 DISCONNECT: {}", accessor.getUser() != null ?
                            accessor.getUser().getName() : "unknown");
                    break;

                default:
                    // 다른 명령어는 그냥 통과
                    break;
            }
        } catch (Exception e) {
            log.error("❌ STOMP 인터셉터 에러 - Command: {}, Error: {}",
                    command, e.getMessage(), e);
            // 예외를 던지지 않고 로깅만 (연결 유지)
            // 필요시 특정 상황에서만 예외 throw
        }

        return message;
    }

    /**
     * CONNECT 명령 처리
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String username = accessor.getFirstNativeHeader("X-Username");
        String userNo = accessor.getFirstNativeHeader("X-User-No");

        log.info("🔑 CONNECT 시도 - X-Username: {}, X-User-No: {}", username, userNo);
        log.debug("All Headers: {}", accessor.toNativeHeaderMap());

        if (username == null || username.isBlank()) {
            log.warn("⚠️ X-Username 헤더 없음 - 익명 사용자로 처리");
            username = "Anonymous_" + System.currentTimeMillis();
        }

        // 인증 객체 생성
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 세션에 저장 (중요!)
        accessor.setUser(auth);
        if (accessor.getSessionAttributes() != null) {
            accessor.getSessionAttributes().put("PRINCIPAL", auth);
            accessor.getSessionAttributes().put("USERNAME", username);
        }

        // SecurityContext 설정
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.info("✅ CONNECT 성공: {}, SessionId: {}", username, accessor.getSessionId());
    }

    /**
     * 다른 명령에서 인증 정보 복구
     */
    private void restoreAuthentication(StompHeaderAccessor accessor) {
        // 이미 User가 설정되어 있으면 스킵
        if (accessor.getUser() != null) {
            log.trace("User already set: {}", accessor.getUser().getName());
            return;
        }

        // 세션에서 복구 시도
        if (accessor.getSessionAttributes() != null) {
            Object principalObj = accessor.getSessionAttributes().get("PRINCIPAL");

            if (principalObj instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("🔄 Principal 복구: {}", auth.getName());
                return;
            }
        }

        // 복구 실패 시 경고만 출력 (예외 던지지 않음)
        log.warn("⚠️ 인증 정보 없음 - Command: {}, SessionId: {}",
                accessor.getCommand(), accessor.getSessionId());
    }
}