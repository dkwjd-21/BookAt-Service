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
import com.bookat.mapper.UserMapper;
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
	private final UserMapper userMapper;
	
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
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
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
	    
	    log.info("refreshToken : {}", refreshToken);
	    boolean valid = jwtTokenProvider.validateToken(refreshToken);
	    log.info("토큰 유효 여부: {}", valid);
	    
	    if(refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레시토큰");
	    }
	    
	    String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
	    User user = userMapper.findUserById(userId);
	    
	    if(user == null) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 없음");
	    }

	    if (!refreshToken.equals(user.getRefreshToken())) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("서버에 저장된 리프레시 토큰과 다름");
	    }

	    String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
		
	    return ResponseEntity.ok(new UserLoginResponse(newAccessToken, null));
	}
	
//	@PostMapping("/logout")
	@PostMapping("/api/user/logout")
	public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken, HttpServletResponse response) {
		String token = accessToken.replace("Bearer ", "");
	    String userId = jwtTokenProvider.getUserIdFromToken(token);
	    
	    log.info("token : {}", token);
	    log.info("userId : {}", userId);

	    User user = userMapper.findUserById(userId);
	    
	    if(user == null) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 없음");
	    }
	    
	    user.setRefreshToken(null);

		Map<String, String> values = new HashMap<>();
		values.put("refreshToken", user.getRefreshToken());
		values.put("userId", user.getUserId());
		userMapper.updateUserRefreshToken(values);

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
