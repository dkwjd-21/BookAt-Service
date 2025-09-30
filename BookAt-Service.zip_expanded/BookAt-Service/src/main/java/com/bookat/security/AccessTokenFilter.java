package com.bookat.security;

import java.io.IOException;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bookat.entity.User;
import com.bookat.mapper.UserLoginMapper;
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
    private final StringRedisTemplate redisTemplate;
    
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String token = resolveToken(request);
		
		if(token != null && jwtTokenProvider.validateToken(token)) {
			String userId = jwtTokenProvider.getUserIdFromToken(token);
			String sidFromToken = jwtTokenProvider.getSidFromToken(token);
			
			String currentSid = redisTemplate.opsForValue().get("user:" + userId + ":current_sid");
			
			// 동시 로그인 감지 시 401 반환 강제로그아웃
			if(currentSid != null && !currentSid.equals(sidFromToken)) {
				log.info("sid 불일치 → 동시 로그인 감지, 강제 로그아웃 처리");
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
			log.info("토큰 없음 or 무효 → 비로그인 상태로 처리 (401 아님)");
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
