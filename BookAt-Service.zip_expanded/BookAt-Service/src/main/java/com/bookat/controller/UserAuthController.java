package com.bookat.controller;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.dto.UserInfoResponse;
import com.bookat.entity.User;
import com.bookat.service.RefreshTokenService;
import com.bookat.service.impl.UserLoginServiceImpl;
import com.bookat.util.CookieUtil;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {
	
	private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final UserLoginServiceImpl service;
    private final StringRedisTemplate redisTemplate;

	// access token 검증 후 userId 전달 (로그인 상태 파악)
	@GetMapping("/validate")
	public ResponseEntity<?> userAuthValidate(Authentication authentication) {
		
		if(authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인 상태 아님"));
		}
		
		User user = (User) authentication.getPrincipal();
		if(user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "사용자 정보 없음")); 
		}
		
		UserInfoResponse userInfo = new UserInfoResponse();
		
	    userInfo.setUserId(user.getUserId());
	    userInfo.setUserName(user.getUserName());
	    userInfo.setPhone(user.getPhone());
	    userInfo.setBirth(user.getBirth());
	    userInfo.setEmail(user.getEmail());
	    
	    return ResponseEntity.ok(userInfo);
	}
	
	// access token 재발급
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
		
		// 쿠키에서 refresh token 찾아 저장
		String refreshToken = cookieUtil.getCookieValue(request, "refreshToken");
	    
		// refresh token 만료 검증
	    if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
	    	// refresh token 만료
	    	return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("error", "refresh token 만료 또는 없음"));
	    }
	    
	    String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
	    
	    // 동시 로그인 검증
	    boolean valid = refreshTokenService.validateRefreshToken(request, response, userId);
	    if(!valid) {
	    	return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("error", "다른 기기에서 로그인"));
	    }
	    
	    User user = service.findUserById(userId);
	    if(user == null) {
	    	return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("error", "사용자가 없음"));
	    }
	    
	    String currentSid = redisTemplate.opsForValue().get("user:" + userId + ":current_sid");
	    if(currentSid == null) {
	    	return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("error", "세션이 만료되었습니다. 다시 로그인해주세요."));
	    }

	    // refresh token 이 유효하다면 새로운 access token 발급
	    String newAccessToken = jwtTokenProvider.generateAccessToken(userId, currentSid);
	    
	    log.info("accessToken 재발급 완료: userId={}", userId);
		
	    return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
	}
	
}
