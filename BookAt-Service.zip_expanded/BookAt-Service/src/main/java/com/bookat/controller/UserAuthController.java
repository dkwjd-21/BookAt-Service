package com.bookat.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.dto.UserInfoResponse;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.service.impl.UserLoginServiceImpl;
import com.bookat.util.CookieUtil;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {
	
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final UserLoginServiceImpl service;

	// access token 검증 후 userId 전달 (로그인 상태 파악)
	@GetMapping("/validate")
	public ResponseEntity<?> userAuthValidate(@RequestHeader(value="Authorization", required = false) String accessToken) {
		String userId = null;
		
		if (accessToken == null || !accessToken.startsWith("Bearer ")) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body("Authorization 헤더가 없거나 잘못되었습니다.");
	    }
		
	    String token = accessToken.substring(7);
	    if (!jwtTokenProvider.validateToken(token)) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 access token입니다.");
	    }
	    
	    userId = jwtTokenProvider.getUserIdFromToken(token);
	    User user = service.findUserById(userId);
	    
	    if(user == null) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("존재하지 않는 사용자입니다.");
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
	public ResponseEntity<?> refresh(HttpServletRequest request) {
		
		String refreshToken = null;
		
		// 쿠키에서 refresh token 찾아 저장
		refreshToken = cookieUtil.getCookieValue(request, "refreshToken");
	    
	    if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
	    	// refresh token 만료
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시토큰");
	    }
	    
	    String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
	    User user = service.findUserById(userId);
	    
	    if(user == null) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 없음");
	    }

	    // refresh token 이 유효하다면 새로운 access token 발급
	    log.info("엑세스토큰 재발급 완료");
	    String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
		
	    return ResponseEntity.ok(new UserLoginResponse(newAccessToken, null));
	}
	
}
