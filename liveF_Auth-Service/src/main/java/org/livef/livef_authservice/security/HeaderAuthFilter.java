package org.livef.livef_authservice.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class HeaderAuthFilter extends OncePerRequestFilter{


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getServletPath();
        String query = request.getQueryString();
        String fullPath = path + (query != null ? "?" + query : "");

        log.info("HeaderAuthFilter 실행 → {} {}", method, fullPath);

        // OPTIONS 요청은 바로 통과
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.info("  → OPTIONS 요청 → 인증 스킵");
            filterChain.doFilter(request, response);
            return;
        }

        // GET /ws/info 요청도 인증 없이 통과
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/ws/info")) {
            log.info("  → GET /ws/info 요청 → 인증 스킵");
            filterChain.doFilter(request, response);
            return;
        }

        String username = request.getHeader("X-Username");
        String role = request.getHeader("X-User-Role");

        log.info("  → X-Username: {}, X-User-Role: {}", username, role);

        if (username == null || username.isBlank()) {
            log.warn("  → X-Username 없음 → 인증 없이 통과 (anyRequest().authenticated()에서 403 가능)");
            filterChain.doFilter(request, response);
            return;
        }

        // 인증 성공
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null && !role.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }

        UserDetails user = User.withUsername(username)
                .password("{noop}N/A")
                .authorities(authorities)
                .accountExpired(false).accountLocked(false)
                .credentialsExpired(false).disabled(false)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("  → 인증 성공 → username={}", username);

        filterChain.doFilter(request, response);
    }
}