package com.bookat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 보호 기능을 비활성화합니다. (POST 요청을 허용하기 위해)
            .csrf(csrf -> csrf.disable())

            // 2. 모든 HTTP 요청에 대해 접근을 허용합니다.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll() 
            );

        return http.build();
    }
}
