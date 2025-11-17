package org.example.livef_chatservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    // ⭐ 챗 서비스가 기대하는 내부 비밀 키 주입
    @Value("${app.security.internal-secret-key}")
    private String expectedSecret;
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        HttpHeaders headers = request.getHeaders();
        String internalSecret = headers.getFirst(INTERNAL_SECRET_HEADER); // HTTP 헤더에서 시크릿 키를 읽음

        // 🔑 1. 내부 시크릿 키 검증 (게이트웨이 우회 방지)
        if (internalSecret == null || !expectedSecret.equals(internalSecret)) {
            log.error("❌ Handshake 거부: 게이트웨이 시크릿 불일치 또는 부재.");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false; // 연결 거부
        }
        log.info("✅ 게이트웨이 시크릿 확인 완료");

        // 2. 사용자 인증 정보 추출 및 세션 저장
        String username = headers.getFirst("X-Username");
        String userNoStr = headers.getFirst("X-User-No");
        String role = headers.getFirst("X-User-Role");

        log.info("WebSocket Handshake: X-Username={}, X-User-No={}", username, userNoStr);

        // 헤더 없으면 익명 처리 (시크릿 키 검증 통과했어도 사용자 정보가 없으면 익명)
        if (username == null || userNoStr == null) {
            log.warn("사용자 정보 없음 → 익명 연결 허용");
            attributes.put("ANONYMOUS", true);
            return true;
        }

        // 인증 정보 저장
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + (role != null ? role.toUpperCase() : "USER"))
        );

        UserDetails user = User.withUsername(username)
                .password("{noop}N/A")
                .authorities(authorities)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, authorities);

        SecurityContext securityContext = new SecurityContextImpl(auth);
        attributes.put("SPRING_SECURITY_CONTEXT", securityContext);
        attributes.put("PRINCIPAL", auth); // StompChannelInterceptor에서 사용될 PRINCIPAL 저장

        log.info("인증된 사용자 연결: {}", username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("Handshake 도중 오류 발생", exception);
        }
    }
}