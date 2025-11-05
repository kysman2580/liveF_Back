package org.example.livef_chatservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getServletPath();

        log.info("================================================================================");
        log.info("🔵 HeaderAuthFilter 시작 → {} {}", method, path);

        // 1. OPTIONS 요청은 무조건 통과 (CORS 사전 요청)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.info("✅ OPTIONS 요청 → 바로 통과");
            filterChain.doFilter(request, response);
            log.info("================================================================================");
            return;
        }

        // 2. ⭐ WebSocket 경로 (/ws/**)는 이 HTTP 필터를 무조건 통과
        if (path.startsWith("/ws")) {
            // Handshake Interceptor에서 인증을 처리하므로, 여기서는 헤더 전달 여부만 로그로 확인
            String username = request.getHeader("X-Username");
            String userNo = request.getHeader("X-User-No");
            String userRole = request.getHeader("X-User-Role");

            log.info("📋 받은 헤더들 (WebSocket 경로):");
            log.info("  X-Username: {}", username != null ? username : "null ❌");
            log.info("  X-User-No: {}", userNo != null ? userNo : "null ❌");
            log.info("  X-User-Role: {}", userRole != null ? userRole : "null ❌");

            // /ws/info, /ws/세션ID/websocket 등 모든 WebSocket 관련 요청은 통과
            log.info("✅ WebSocket 경로 감지 → 인증 로직 건너뛰고 통과 (HandshakeInterceptor 예정)");
            filterChain.doFilter(request, response);
            log.info("================================================================================");
            return;
        }

        // 3. HTTP 요청 (WebSocket이 아닌 일반 API 요청)에 대한 헤더 인증 시도
        String username = request.getHeader("X-Username");
        String userNo = request.getHeader("X-User-No");
        String role = request.getHeader("X-User-Role");

        log.info("📋 받은 헤더들 (HTTP 경로):");
        log.info("  X-Username: {}", username != null ? username : "null ❌");
        log.info("  X-User-No: {}", userNo != null ? userNo : "null ❌");
        log.info("  X-User-Role: {}", role != null ? role : "null ❌");


        if (username == null || username.isBlank() || userNo == null || userNo.isBlank()) {
            log.warn("⚠️ 필수 인증 헤더 (X-Username/X-User-No) 없음 → 익명으로 통과");
            filterChain.doFilter(request, response);
            log.info("================================================================================");
            return;
        }

        // 4. 인증 성공
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        UserDetails user = User.withUsername(username)
                .password("{noop}N/A") // JWT 기반이므로 비밀번호는 사용하지 않음
                .authorities(authorities)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("✅ HTTP 헤더 기반 인증 성공 → username={}", username);

        filterChain.doFilter(request, response);
        log.info("================================================================================");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // ⭐ WebSocket 및 SockJS 메타데이터는 필터 건너뜀
        boolean isWebSocketPath = path.startsWith("/ws/info") ||  // ⭐ 이 줄 추가
                path.startsWith("/ws") ||
                path.startsWith("/app") ||
                path.startsWith("/topic");

        if (isWebSocketPath) {
            log.info("⏭️ [HeaderAuthFilter] WebSocket/SockJS 경로 감지 → 필터 건너뜀: {}", path);
        }

        return isWebSocketPath;
    }
}