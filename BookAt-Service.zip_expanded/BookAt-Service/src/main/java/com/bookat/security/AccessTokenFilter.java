package com.bookat.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bookat.entity.User;
import com.bookat.mapper.UserLoginMapper;
import com.bookat.util.JwtRedisUtil;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenFilter extends OncePerRequestFilter {
	
    private final JwtTokenProvider jwtTokenProvider;
    private final UserLoginMapper userMapper;
    private final JwtRedisUtil jwtRedisUtil;
    
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String token = resolveToken(request);
		
		if(token != null && jwtTokenProvider.validateToken(token)) {
			String userId = jwtTokenProvider.getUserIdFromToken(token);
			String sidFromToken = jwtTokenProvider.getSidFromToken(token);
			String currentSid = jwtRedisUtil.getCurrentSid(userId);
			
			if(sidFromToken == null || currentSid == null || !currentSid.equals(sidFromToken)) {
				// sidFromToken == null : 잘못된 토큰, 액세스 토큰 재발급 불가
				// currentSid == null : refresh token 이용해서 재로그인/재발급 가능
				// sid 불일치 : 액세스 토큰 재발급 불가, 강제 로그아웃
				log.info("액세스 토큰 sid 검증 실패 (sidFromToken={}, currentSid={}) → 401 반환", sidFromToken, currentSid);
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			
			// 정상 로그인 상태
			User user = userMapper.findUserById(userId);
			if(user != null) {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, null);
				SecurityContextHolder.getContext().setAuthentication(authentication);
				log.info("access token 인증 성공 : {}", userId);
			}
		} else {
			// 토큰이 없거나 무효라면 비로그인상태로 통과 (401 아님)
			log.info("토큰 없음 or 무효 → 비로그인 상태로 처리");
		}
		
		filterChain.doFilter(request, response);
	}
	
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 정적 리소스 제외
        if (path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images") || path.startsWith("/favicon.ico")) {
            return true;
        }
        
        if (path.startsWith("/.well-known/")) {
            return true;
        }

        return false;
    }
	
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

}
