package com.bookat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        		// 개발용 임시허용 
        		.requestMatchers("/reservation/seat/**").permitAll()
        		.requestMatchers("/css/**", "/js/**", "/images/**").permitAll()			// 정적 리소스 접근 가능
		.requestMatchers("/", "/user/**", "/auth/**", "/books/**", "/events/**", "/infoPage/**", "/cart/**").permitAll()
				.requestMatchers("/error/**", "/payment/success", "/reservation/*/cancel", "/payment/dev/**").permitAll()
        		.requestMatchers("/api/**", "/queue/**", "/reservation/**", "/myPage/**", "/order/**", "/order/direct/**",
        				"/payment/**",
        				"/payment/session/start-event",
        				"/payment/session/start",
        				"/payment/session/start-cart",
                        "/payment/session/context",
                        "/payment/api/complete","/reservation/**",
						"/myPage/orderList", "/myPage/orderList/**").authenticated()
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