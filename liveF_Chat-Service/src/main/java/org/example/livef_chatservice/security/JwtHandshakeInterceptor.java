package org.example.livef_chatservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
// ✅ HandshakeInterceptor 구현
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    // Handshake 전에 호출됨 (HTTP 요청 처리)
    // JwtHandshakeInterceptor.java (핵심 수정)
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        HttpHeaders headers = request.getHeaders();
        String username = headers.getFirst("X-Username");
        String userNoStr = headers.getFirst("X-User-No");
        String role = headers.getFirst("X-User-Role");

        log.info("WebSocket Handshake: X-Username={}, X-User-No={}", username, userNoStr);

        // 헤더 없어도 무조건 통과 (익명 허용)
        if (username == null || userNoStr == null) {
            log.warn("헤더 없음 → 익명 연결 허용");
            attributes.put("ANONYMOUS", true);
            return true; // 반드시 true
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
        attributes.put("PRINCIPAL", auth);

        log.info("인증된 사용자 연결: {}", username);
        return true;
    }

    // Handshake 이후 호출됨 (필요 없으면 비워둡니다)
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("Handshake 도중 오류 발생", exception);
        }
    }
}