package org.example.livef_chatservice.security;

import lombok.extern.slf4j.Slf4j;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String path = request.getURI().getPath();
        log.info("================================================================================");
        log.info("🔵 WebSocket Handshake 요청 경로: {}", path);

        // Gateway에서 추가한 헤더 읽기
        String username = request.getHeaders().getFirst("X-Username");
        String userNo = request.getHeaders().getFirst("X-User-No");
        String role = request.getHeaders().getFirst("X-User-Role");

        log.info("📋 Handshake 헤더 확인:");
        log.info("  X-Username: {}", username != null ? username : "null ❌");
        log.info("  X-User-No: {}", userNo != null ? userNo : "null ❌");
        log.info("  X-User-Role: {}", role != null ? role : "null ❌");


        // 1. INFO 요청 또는 헤더가 없는 경우: 익명 사용자로 처리
        if (path.contains("/info") || username == null || username.isBlank()) {

            if (path.contains("/info")) {
                log.info("✅ INFO 요청 감지 → 인증 없이 통과");
            } else {
                log.warn("⚠️ 인증 헤더 없음 → 익명 사용자(anonymous)로 연결 허용");
            }

            log.info("================================================================================");
            return true; // 연결은 허용
        }

        // 2. 인증된 사용자 처리
        try {
            List<GrantedAuthority> authorities = new ArrayList<>();
            String finalRole = (role != null && !role.isBlank()) ? role.toUpperCase() : "USER";
            authorities.add(new SimpleGrantedAuthority("ROLE_" + finalRole));

            UserDetails userDetails = User.withUsername(username)
                    .password("{noop}N/A")
                    .authorities(authorities)
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            // ⭐ 핵심: Spring Security가 인식하는 SecurityContext 객체로 변환하여 저장
            SecurityContext securityContext = new SecurityContextImpl(authentication);
            attributes.put("SPRING.SECURITY.CONTEXT", securityContext);

            // 추가적인 사용자 정보도 WebSocket Session에 저장
            attributes.put("username", username);
            attributes.put("memberNo", userNo != null ? Long.parseLong(userNo) : 0L);
            attributes.put("role", finalRole);
            attributes.put("authenticated", true);
            attributes.put("authentication", authentication);

            log.info("✅ 인증 성공: {} (권한: [ROLE_{}])", username, finalRole);

        } catch (NumberFormatException e) {
            log.error("❌ X-User-No 파싱 오류 발생: {}", userNo, e);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            log.info("================================================================================");
            return false; // 인증 정보 파싱 오류 시 연결 거부
        }

        log.info("================================================================================");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("❌ Handshake 실패: {}", exception.getMessage());
        }
    }
}