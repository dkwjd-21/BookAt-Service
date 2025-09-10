package com.bookat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.service.impl.UserLoginServiceImpl;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {
	
    private final JwtTokenProvider jwtTokenProvider;
    private final UserLoginServiceImpl service;

	// access token 검증 후 userId 전달
	@PostMapping("/validate")
	public ResponseEntity<String> userAuthValidate(@RequestHeader(value="Authorization", required=false) String accessToken, HttpServletResponse response) {
		String userId = null;
		
		if (accessToken == null || !accessToken.startsWith("Bearer ")) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body("Authorization 헤더가 없거나 잘못되었습니다.");
	    }
		
		String token = accessToken.substring(7);

		try {
	        // 토큰 유효성 검증 (만료, 서명 체크)
	        if (!jwtTokenProvider.validateToken(token)) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 access token입니다.");
	        }

	        // 토큰에서 userId 추출
	        userId = jwtTokenProvider.getUserIdFromToken(token);

	        // 검증 성공 200
	        return ResponseEntity.ok("access token 유효함. 사용자 ID: " + userId);

	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("access token 검증 실패");
	    }
	}
	
	// access token 재발급
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest request) {
		
		String refreshToken = null;
		
		// 쿠키에서 refresh token 찾아 저장
	    if (request.getCookies() != null) {
	        for (Cookie cookie : request.getCookies()) {
	            if (cookie.getName().equals("refreshToken")) {
	                refreshToken = cookie.getValue();
	            }
	        }
	    }
	    
	    if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
	    	// refresh token 만료
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시토큰");
	    }
	    
	    String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
	    User user = service.findUserById(userId);
	    
	    if(user == null) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 없음");
	    }

//	    if (!refreshToken.equals(user.getRefreshToken())) {
//	    	// 디비에 저장된 refresh token 이랑 일치하는지 확인.
//	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("서버에 저장된 리프레시 토큰과 다름");
//	    }

	    // refresh token 이 유효하다면 새로운 access token 발급
	    String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
		
	    return ResponseEntity.ok(new UserLoginResponse(newAccessToken, null));
	}
	
}
