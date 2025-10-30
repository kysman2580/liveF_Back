package org.livef.livef_apigateway.security;


import org.livef.livef_apigateway.component.JwtAuthenticationWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain security(ServerHttpSecurity httpSecurity, JwtAuthenticationWebFilter jwtFilter) {
    	log.info("{}","잘들어왔나요?/");
    	
        return httpSecurity
            .csrf(csrf -> csrf.disable())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()) //session STATELESS
            .addFilterAfter(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            // 라우트 허용/차단 // 사용자 권한 별 접근도 설정 가능
            .authorizeExchange(ex -> ex
            	.pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/ws/**").permitAll()  // ✅ 추가!

            	.pathMatchers("/api/auth/login", "/api/auth/refresh","/api/member/sign-up").permitAll()
                .pathMatchers(HttpMethod.PUT,   "/api/**").authenticated()
                .pathMatchers(HttpMethod.PATCH, "/api/**").authenticated()
                .pathMatchers(HttpMethod.DELETE,"/api/**").authenticated()
                .pathMatchers(HttpMethod.GET,"/api/**").authenticated()
                )
          .build();
                  
    }
    
    

}