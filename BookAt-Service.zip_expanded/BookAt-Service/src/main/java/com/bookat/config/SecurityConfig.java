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

import com.bookat.security.AccessTokenFilter;
import com.bookat.security.RefreshTokenFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
	
    private final AccessTokenFilter accessTokenFilter;
    private final RefreshTokenFilter refreshTokenFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	
    	log.info("-- securityFilterChain --");
    	
        http
        .csrf(csrf -> csrf.disable())
        .formLogin(AbstractHttpConfigurer::disable)
//        .httpBasic(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
        		.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()	// 정적 리소스 접근 가능
        		.requestMatchers("/mainpage/**", "/books/**", "/event/**","/cart/**").permitAll()
        		.requestMatchers("/pay/**").authenticated()
        		.requestMatchers("/", "/user/**", "/auth/**", "/mainPage/**", "/infoPage/**").permitAll()
        					// 홈, 로그인, 메인페이지, 상세페이지 토큰없이 접근 허용
        		.requestMatchers("/api/**", "/queue/**", "/myPage/**","/order/**").authenticated()
        							// 예약 기능 토큰 필요
//        		.requestMatchers("/books/**", "/events/**").authenticated()
                .anyRequest().denyAll()
       ).addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(refreshTokenFilter, AccessTokenFilter.class);
		
		return http.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
