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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.exception.LoginException;
import com.bookat.service.impl.UserLoginServiceImpl;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
//@RestController
//@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLoginController {

    private final JwtTokenProvider jwtTokenProvider;
	private final UserLoginServiceImpl loginService;
	
	@GetMapping("/")
	public String home() {
		return "home";
	}
	
	// 로그인
//	@GetMapping("/login")
	@GetMapping("/api/user/login")
	public String loginForm(Model model) {
		
		model.addAttribute("userLogin", new UserLoginRequest());
		
		return "user/loginForm";
	}
	
//	@PostMapping("/login")
	@PostMapping("/api/user/login")
	public ResponseEntity<?> login(@Valid @RequestBody UserLoginRequest userLoginRequest, BindingResult bindingResult, HttpServletResponse response) {
		
		// input 에 값을 입력 안하고 요청할 시 검증
		if(bindingResult.hasErrors()) {
			String errMsg = bindingResult.getFieldError().getDefaultMessage();
			return ResponseEntity.badRequest().body(errMsg);
		}
		
		try {
			// refreshToken 서비스에서 디비에 저장 (지금은 비활성화)
			UserLoginResponse tokens = loginService.login(userLoginRequest);
			
			// refreshToken 쿠키 저장
			Cookie refreshCookie  = new Cookie("refreshToken", tokens.getRefreshToken());
			refreshCookie .setHttpOnly(true);
//			refreshCookie .setSecure(true);
			refreshCookie .setPath("/");
			refreshCookie .setMaxAge(60 * 60 * 24 * 7);	// 7일
			response.addCookie(refreshCookie);
			
			// accessToken 은 localStorage 에 저장
			return ResponseEntity.ok(new UserLoginResponse(tokens.getAccessToken(), null));
		} catch (LoginException le) {
			
			// 사용자가 없거나 비밀번호 불일치
		    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(le.getMessage());
		}
	}
	
	// 로그아웃
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
		
		// 디비에서도 삭제하기 위함
		if(userId != null) {
//	        User user = loginService.findUserById(userId);
//	        if(user != null) {
//	            user.setRefreshToken(null);
//	            loginService.refreshTokenUpdate(user.getRefreshToken(), user.getUserId());
//	        }
	    }

		// 쿠키 삭제
	    Cookie refreshCookie = new Cookie("refreshToken", null);
	    refreshCookie.setHttpOnly(true);
	    refreshCookie.setMaxAge(0);
	    refreshCookie.setPath("/");
	    response.addCookie(refreshCookie);

	    return ResponseEntity.ok("로그아웃 성공");
	}
	
	// 아이디 찾기
	@GetMapping("/api/user/findId")
	public String findIdForm() {
		
		return "user/findIdForm";
	}
	
	// 비밀번호 찾기
	@GetMapping("/api/user/findPw")
	public String findPwCheckForm() {
		return "user/findPasswordForm";
	}
	
	// =============================================================================================
	/*
	@PostMapping("/api/user/findPw")
	public String findPwCheck(@Valid @ModelAttribute FindPassword inputUserInfo, BindingResult bindingResult, Model model) {
		
		if(bindingResult.hasErrors()) {
			return "/user/findPwForm";
		}
		
		User findUser = loginService.findUserById(inputUserInfo.getUserId());
		
		if(findUser == null) {
			
			log.info("존재하지 않는 아이디");
			bindingResult.rejectValue("userId", "id not found", "존재하지 않는 아이디입니다.");
			return "/user/findPwForm";
			
		} else {

			String dbPhone = findUser.getPhone() != null ? findUser.getPhone().replaceAll("-", "") : "";
			
			if(!inputUserInfo.getPhone().equals(dbPhone)) {
				log.info("유저는 있는데 전화번호는 불일치");
				bindingResult.rejectValue("phone", "phone mismatch", "전화번호가 일치하지 않습니다.");
				return "/user/findPwForm";
			}
			
			log.info("비밀번호 변경화면으로");
			
			InputUserPassword inputUserPassword = new InputUserPassword();
			inputUserPassword.setUserId(findUser.getUserId());
			model.addAttribute("inputUserPw", inputUserPassword);
			
			return "/user/inputPwForm";
			
		}
		
	}
	
	@PostMapping("/api/user/changePassword")
	public String changePassword(@ModelAttribute InputUserPassword inputUserPassword, BindingResult bindingResult) {
		
		if(bindingResult.hasErrors()) {
			return "/user/inputPwForm";
		}
		
		User user = loginService.findUserById(inputUserPassword.getUserId());
		
		if(user == null) {
			log.info("아이디 없음");
			return "redirect:/api/user/login";
		} else {
			user.setUserPw(inputUserPassword.getPassword());
			loginService.updatePassword(user.getUserPw(), user.getUserId());
			return "user/resultPwForm";
		}
	}
	*/
	// =============================================================================================

	@PostMapping("/api/user/findPw")
	@ResponseBody
    public Map<String,Object> findPwCheck(@RequestParam String userId, @RequestParam String phone) {
        Map<String,Object> result = new HashMap<>();

        User user = loginService.findUserById(userId);
        if(user == null) {
            result.put("success", false);
            result.put("message", "존재하지 않는 아이디입니다.");
            return result;
        }

        String dbPhone = user.getPhone() != null ? user.getPhone().replaceAll("-", "") : "";
        if(!phone.equals(dbPhone)) {
            result.put("success", false);
            result.put("message", "전화번호가 일치하지 않습니다.");
            return result;
        }

        result.put("success", true);
        result.put("userId", user.getUserId());
        
        return result;
    }

    @PostMapping("/api/user/changePassword")
    @ResponseBody
    public Map<String,Object> changePassword(@RequestParam String userId, @RequestParam String password) {
        Map<String,Object> result = new HashMap<>();

        User user = loginService.findUserById(userId);
        if(user == null) {
            result.put("success", false);
            result.put("message", "존재하지 않는 아이디입니다.");
            return result;
        }

        user.setUserPw(password);
        loginService.updatePassword(user.getUserPw(), user.getUserId());

        result.put("success", true);
        
        return result;
    }

}
