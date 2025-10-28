package com.example.demo.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

<<<<<<< HEAD
        http
                .csrf((auth) -> auth.disable());
        http
                .authorizeHttpRequests((auth) -> auth.anyRequest().authenticated());
        http
                .httpBasic(Customizer.withDefaults());
        return http.build();
=======
    	return http
                .csrf((auth) -> auth.disable())
                .authorizeHttpRequests((auth) -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
>>>>>>> 48d55ada7585b7a5eaec46264f25dfb8018ccc1a
    }

    @Bean
    public UserDetailsService userDetailsService() {

        UserDetails user1 = User.builder()
                .username("admin")
                .password(bCryptPasswordEncoder().encode("1234"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user1);
    }

}