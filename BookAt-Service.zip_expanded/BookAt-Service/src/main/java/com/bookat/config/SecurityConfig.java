package com.bookat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.bookat.security.JwtAuthenticationFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    	
    	log.info("-- securityFilterChain --");
    	
        http
        .csrf(csrf -> csrf.disable())
        .formLogin(AbstractHttpConfigurer::disable)
//        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
        		.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()	// 정적 리소스 접근 가능
        		.requestMatchers("/", "/user/**", "/auth/**").permitAll()		// 로그인 전 접근 가능 (페이지들)
        		.requestMatchers("/queue/**").permitAll()		// 기능개발용 임시 허용
        		.requestMatchers("/api/captcha/**").permitAll()
        		.requestMatchers("/api/**").authenticated()						// 기능들
                .anyRequest().denyAll()
        ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
