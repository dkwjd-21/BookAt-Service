package com.bookat.security;

import java.io.IOException;

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
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
    private final JwtTokenProvider jwtTokenProvider;
    private final UserLoginMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("-- JwtAuthenticationFilter: {} --", path);

        // permitAll 경로는 그냥 통과
        if (path.startsWith("/api/user/") ||
        		path.startsWith("/user/") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/") ||
            path.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 추출
        String token = resolveToken(request);

        // 토큰이 있으면 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userMapper.findUserById(userId);

            if (user != null) {
                log.info("인증 성공: {}", user.getUserId());

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

        // 필터 제외할 API
        if (path.equals("/api/user/refresh") || path.equals("/api/user/logout")) {
            return true;
        }

        // 정적 리소스 제외
        if (path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images") || path.equals("/favicon.ico")) {
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
