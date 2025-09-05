package com.bookat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.exception.LoginException;
import com.bookat.service.impl.UserLoginServiceImpl;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
//@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLoginController {

    private final JwtTokenProvider jwtTokenProvider;
	private final UserLoginServiceImpl loginService;
	
	@GetMapping("/")
	public String home() {
		return "home";
	}
	
//	@GetMapping("/login")
	@GetMapping("/api/user/login")
	public String loginForm(Model model) {
		
		model.addAttribute("userLogin", new UserLoginRequest());
		
		return "user/loginForm";
	}
	
//	@PostMapping("/login")
	@PostMapping("/api/user/login")
	public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest userLoginRequest, BindingResult bindingResult, HttpServletResponse response) {
		
		if(bindingResult.hasErrors()) {
			String errMsg = bindingResult.getFieldError().getDefaultMessage();
			return ResponseEntity.badRequest().body(errMsg);
		}
		
		try {
			UserLoginResponse tokens = loginService.login(userLoginRequest);
			
			// refreshToken 쿠키 저장
			Cookie refreshCookie  = new Cookie("refreshToken", tokens.getRefreshToken());
			refreshCookie .setHttpOnly(true);
//			refreshCookie .setSecure(true);
			refreshCookie .setPath("/");
			refreshCookie .setMaxAge(60 * 60 * 24 * 7);
			response.addCookie(refreshCookie);
			
			// accessToken localStorage 에 저장
			return ResponseEntity.ok(new UserLoginResponse(tokens.getAccessToken(), null));
		} catch (LoginException le) {
			// 사용자가 없거나 비밀번호 불일치

		    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(le.getMessage());
		}
	}
	
//	@PostMapping("/refresh")
	@PostMapping("/api/user/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest request) {
		
		String refreshToken = null;
	    if (request.getCookies() != null) {
	        for (Cookie cookie : request.getCookies()) {
	            if (cookie.getName().equals("refreshToken")) {
	                refreshToken = cookie.getValue();
	            }
	        }
	    }
	    
	    if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
	    	// 401에러
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시토큰");
	    }
	    
	    String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
	    User user = loginService.findUserById(userId);
	    
	    if(user == null) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 없음");
	    }

	    if (!refreshToken.equals(user.getRefreshToken())) {
	    	// 401에러
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("서버에 저장된 리프레시 토큰과 다름");
	    }

	    String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
		
	    return ResponseEntity.ok(new UserLoginResponse(newAccessToken, null));
	}
	
//	@PostMapping("/logout")
	@PostMapping("/api/user/logout")
	public ResponseEntity<String> logout(@RequestHeader(value="Authorization", required=false) String accessToken, HttpServletResponse response) {
		String userId = null;
		
		if(accessToken != null && accessToken.startsWith("Bearer ")) {
	        try {
	            String token = accessToken.replace("Bearer ", "");
	            userId = jwtTokenProvider.getUserIdFromToken(token); // 만료 토큰도 파싱 가능하도록 구현
	        } catch(Exception e) {
	            log.warn("Access Token 파싱 실패, 로그아웃 계속 진행");
	        }
	    }
		
		if(userId != null) {
	        User user = loginService.findUserById(userId);
//	        if(user != null) {
//	            user.setRefreshToken(null);
//	            loginService.refreshTokenUpdate(user.getRefreshToken(), user.getUserId());
//	        }
	    }

	    Cookie refreshCookie = new Cookie("refreshToken", null);
	    refreshCookie.setHttpOnly(true);
	    refreshCookie.setMaxAge(0);
	    refreshCookie.setPath("/");
	    response.addCookie(refreshCookie);

	    return ResponseEntity.ok("로그아웃 성공");
	}
	
	@GetMapping("/api/user/findId")
	public String findIdForm() {
		return "user/findIdForm";
	}
	
	@GetMapping("/api/user/findPw")
	public String findPwForm() {
		return "user/findPwForm";
	}
}
