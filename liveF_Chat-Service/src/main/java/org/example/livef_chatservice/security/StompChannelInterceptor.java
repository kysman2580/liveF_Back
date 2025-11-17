package org.example.livef_chatservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StompChannelInterceptor implements ChannelInterceptor {

    @Value("${app.security.internal-secret-key}")
    private String expectedSecret;
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private static final List<StompCommand> AUTH_REQUIRED_COMMANDS = List.of(
            StompCommand.SUBSCRIBE, StompCommand.SEND, StompCommand.MESSAGE
    );

    @PostConstruct
    public void logLoadedSecret() {
        log.error("🔑 [CHAT] Loaded Secret Key (EXPECTED): [{}]", expectedSecret);
    }


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor == null || accessor.getCommand() == null) {
            log.trace("Skip: accessor or command is null (heartbeat or polling)");
            return message;
        }

        StompCommand command = accessor.getCommand();
        log.info("📨 [STOMP] Command: {}, SessionId: {}", command, accessor.getSessionId());

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
                    break;
            }
        } catch (MessageDeliveryException e) {
            log.error("❌ STOMP 메시지 거부됨 - Command: {}, Error: {}", command, e.getMessage());
            throw e; // MessageDeliveryException 발생 시 연결 강제 종료
        } catch (Exception e) {
            log.error("❌ STOMP 인터셉터 에러 - Command: {}, Error: {}",
                    command, e.getMessage(), e);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        // ⭐⭐⭐ 내부 시크릿 검증 로직 제거됨: 이제 JwtHandshakeInterceptor에서 처리합니다. ⭐⭐⭐

        // Handshake Interceptor가 저장한 인증 객체 복구
        if (accessor.getSessionAttributes() != null) {
            Object principalObj = accessor.getSessionAttributes().get("PRINCIPAL");

            if (principalObj instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth); // STOMP 세션에 사용자 설정
                // SecurityContextHolder는 restoreAuthentication에서 처리하는 것이 일반적이지만,
                // CONNECT 단계에서 바로 설정하여 로깅 등에 활용할 수 있습니다.
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("✅ CONNECT 성공 (Handshake 인증 복구): {}", auth.getName());
                return;
            }

            // 익명 연결 여부 확인 (JwtHandshakeInterceptor에서 설정한 ANONYMOUS 플래그)
            if (accessor.getSessionAttributes().get("ANONYMOUS") != null) {
                log.info("✅ CONNECT 성공: 익명 사용자");
                return;
            }
        }

        // 인증 정보가 복구되지 않은 경우 (보안상 거부)
        log.error("❌ CONNECT 거부: Handshake 인증 정보 없음. SessionId: {}", accessor.getSessionId());
        throw new MessageDeliveryException("Authentication required.");
    }

    private void restoreAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            log.trace("User already set: {}", accessor.getUser().getName());
            return;
        }

        if (accessor.getSessionAttributes() != null) {
            Object principalObj = accessor.getSessionAttributes().get("PRINCIPAL");
            if (principalObj instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("🔄 Principal 복구: {}", auth.getName());
                return;
            }
        }

        log.warn("⚠️ 인증 정보 복구 실패 - Command: {}, SessionId: {}",
                accessor.getCommand(), accessor.getSessionId());

        if (AUTH_REQUIRED_COMMANDS.contains(accessor.getCommand())) {
            log.error("❌ 인증 정보 없음: 필수 명령어({}) 처리 거부", accessor.getCommand());
            throw new MessageDeliveryException("Authentication required for this command.");
        }
    }
}