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
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
//        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
        		.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()	// 정적 리소스 접근 가능
        		.requestMatchers("/", "/user/**", "/api/user/**", "/api/auth/**").permitAll()	// 로그인 전 접근 가능
        		// 로그인 상태 접속 (후에 뷰랑 api 랑 분리? -> 뷰는 permitAll, 기능은 authenticated)
        		.requestMatchers("/api/pay/**").authenticated()
                .anyRequest().denyAll()
        ).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//        JWT 필터는 인증이 필요한 URL만 통과시키도록 되어 있어야 함
//        permitAll URL은 필터에서 인증 체크 없이 그냥 지나가도록 구현되어야 안전
		
		return http.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
