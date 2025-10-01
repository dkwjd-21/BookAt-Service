package com.bookat.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bookat.entity.User;
import com.bookat.mapper.UserLoginMapper;
import com.bookat.util.CookieUtil;
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
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	// 필터로 사용 X, 계속 필요없으면 나중에 삭제할 예정
	
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final UserLoginMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("JwtAuthenticationFilter: {}", path);

        // 토큰 추출
        String token = resolveToken(request);

        // 토큰이 있으면 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userMapper.findUserById(userId);

            if (user != null) {
                log.info("인증 성공: {}", user.getUserId());
                
                // 동시 로그인 필터
                String refreshToken = cookieUtil.getCookieValue(request, "refreshToken");
                String loginTime = cookieUtil.getCookieValue(request, "loginTime");
                
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
                Map<String, String> redisValue = hashOps.entries(userId);
                
                if(redisValue != null && !redisValue.isEmpty()) {
                    String redisRefreshToken = redisValue.get("refreshToken");
                    String redisLoginTime = redisValue.get("loginTime");
                    
                    // RefreshToken 만료 여부 체크
                    if (redisRefreshToken != null && !jwtTokenProvider.validateToken(redisRefreshToken)) {
                        log.info("RefreshToken 만료 → Redis 삭제");
                        redisTemplate.delete(userId);
                        cookieUtil.deleteCookie(response, "refreshToken");
                        cookieUtil.deleteCookie(response, "loginTime");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    if (redisRefreshToken != null && redisLoginTime != null) {
                        if (!refreshToken.equals(redisRefreshToken) || !loginTime.equals(redisLoginTime)) {
                            log.info("다른 기기에서 로그인됨 → 강제 로그아웃");
                            cookieUtil.deleteCookie(response, "accessToken");
                            cookieUtil.deleteCookie(response, "refreshToken");
                            cookieUtil.deleteCookie(response, "loginTime");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                    }
                }

                // 인증 컨텍스트 설정
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, null);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            log.info("토큰 없음 또는 유효하지 않음");
        }

        // 비로그인 상태도 그냥 통과 (403 아님)
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.equals("/") || path.equals("/auth/refresh") || path.equals("/user/login") || path.equals("/user/logout") || path.equals("/queue/reservation")) {
            return true;
        }

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

