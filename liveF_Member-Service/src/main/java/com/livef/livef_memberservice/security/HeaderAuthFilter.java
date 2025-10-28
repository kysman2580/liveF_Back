package com.livef.livef_memberservice.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.livef.livef_memberservice.util.vo.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
    	String username = request.getHeader("X-Username");
        String role     = request.getHeader("X-User-Role");
        String userNo   = request.getHeader("X-User-No");
        
        log.info("{}","잘들어왔나요?/");
        // 헤더가 없으면 익명으로 통과
        if (username == null || username.isBlank()) {
          filterChain.doFilter(request, response);
          return;
        }
        
        Long memberNo = null;
        if (userNo != null && !userNo.isBlank()) {
            try {
                memberNo = Long.valueOf(userNo);             // 문자열 → Long
            } catch (NumberFormatException e) {
                // 잘못된 헤더 값이면 인증 없이 통과하거나, 401 처리 중 하나 선택
            	filterChain.doFilter(request, response);
                return;
            }
        }
        
        List<GrantedAuthority> authorities = new ArrayList();
        
        CustomUserDetails user = CustomUserDetails.builder()
                .memberNo(memberNo)          // PK 넣기
                .username(username)          // memberId
                .password("N/A")             // 사용 안 함
                .isActive("Y")                // 필요시 헤더로 받아 변환
                .authorities(authorities)
                .build();
        
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        filterChain.doFilter(request, response);
    }
}