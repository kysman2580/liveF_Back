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
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    /* -------------------------------------------------------------
       1. shouldNotFilter – WebSocket, SockJS, OPTIONS 요청 완전 제외
       ------------------------------------------------------------- */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();

        boolean isWebSocketPath = path.startsWith("/ws") ||
                path.startsWith("/app") ||
                path.startsWith("/topic");

        boolean isOptions = "OPTIONS".equalsIgnoreCase(method);

        if (isWebSocketPath || isOptions) {
            log.debug("HeaderAuthFilter 건너뜀: {} {} (WebSocket/OPTIONS)", method, path);
            return true;
        }

        return false;
    }

    /* -------------------------------------------------------------
       2. doFilterInternal – HTTP API 인증 전용
       ------------------------------------------------------------- */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        if (path.startsWith("/ws") || path.contains("/info")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 나머지 HTTP API에만 인증 적용
        String username = request.getHeader("X-Username");
        String userNoStr = request.getHeader("X-User-No");

        if (username != null && userNoStr != null) {
            // 인증 정보 생성
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(Map.of("userNo", userNoStr));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}