package com.bookat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.exception.LoginException;
import com.bookat.service.RefreshTokenService;
import com.bookat.service.impl.UserLoginServiceImpl;
import com.bookat.util.CookieUtil;
import com.bookat.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserLoginController {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
	private final UserLoginServiceImpl loginService;
	private final RefreshTokenService refreshTokenService;
	
	// 아이디 찾기 간편인증 정보
    @Value("${portone.public.store-id}")
    private String portoneStoreId;
    @Value("${portone.public.channel-key}")
    private String portoneChannelKey;
	@Value("${portone.api_secret}")
	private String portoneApiSecret;
	
	// 로그인 페이지로 이동
	@GetMapping("/login")
	public String loginForm() {
		
		return "user/loginForm";
	}
	
	// 로그인 처리
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletResponse response) {
		
		try {
			UserLoginResponse tokens = loginService.login(userLoginRequest);
			String userId = userLoginRequest.getUserId();
			String refreshToken = tokens.getRefreshToken();
			String loginTime = String.valueOf(System.currentTimeMillis());
			
			// redis 에 refresh token 과 loginTime 저장
			refreshTokenService.storeRefreshToken(userId, refreshToken, loginTime);
			
			// 쿠키 에 refresh token 과 loginTime 저장
			cookieUtil.createCookie(response, "refreshToken", refreshToken);
			cookieUtil.createCookie(response, "loginTime", loginTime);
			
			return ResponseEntity.ok(Map.of("accessToken", tokens.getAccessToken()));
		} catch (LoginException le) {
			// 사용자가 없거나 비밀번호 불일치
		    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", le.getMessage()));
		} catch (Exception e) {
			log.error("로그인 처리 중 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "로그인 처리 중 오류가 발생했습니다."));
		}
	}
	
	// 로그아웃
	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestHeader(value="Authorization", required=false) String accessToken, @CookieValue(value = "loginTime", required = false) String loginTimeCookie, HttpServletResponse response) {
		String userId = null;
		
		if(accessToken != null && accessToken.startsWith("Bearer ")) {
	        try {
	            String token = accessToken.replace("Bearer ", "");
	            userId = jwtTokenProvider.getUserIdFromToken(token);
	        } catch(Exception e) {
	            log.warn("access token 파싱 실패, 로그아웃 계속 진행");
	        }
	    }

		// redis 세션 삭제 (loginTime 비교 없이 바로 삭제)
		if(userId != null) {
			loginService.deleteSessionInfo(userId);
		}
	    
		// 관련 쿠키 삭제
	    String[] cookieNames = {"refreshToken", "loginTime"};
	    for (String name : cookieNames) {
	    	cookieUtil.deleteCookie(response, name);
	    }
	    
	    SecurityContextHolder.clearContext();
	    
	    return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
	}
	
	// 아이디 찾기 페이지로 이동
	@GetMapping("/findId")
	public String findIdForm(Model model) {
        model.addAttribute("portoneStoreId", portoneStoreId);
        model.addAttribute("portoneChannelKey", portoneChannelKey);
        model.addAttribute("portoneApiSecret", portoneApiSecret);
		
		return "user/findIdForm";
	}
	
	// 간편인증으로 사용자 검증
	@PostMapping("/verify")
	@ResponseBody
    public ResponseEntity<Map<String, Object>> verifyIdentity(
            @RequestBody Map<String, String> payload, HttpSession session) {
        
        Map<String, Object> responseBody = new HashMap<>();
        
        try {
            String identityVerificationId = payload.get("identityVerificationId");
            String apiUrl = "https://api.portone.io/identity-verifications/" + identityVerificationId;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            
            headers.set("Authorization", "PortOne " + portoneApiSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, entity, String.class);
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
           
            String status = root.path("status").asText();

            log.info("PortOne 응답 데이터: {}", response.getBody());
            
            if ("VERIFIED".equals(status)) {
                // 인증 성공
            	JsonNode customerNode = root.path("verifiedCustomer");
                String name = customerNode.path("name").asText();
                // 숫자로만 구성된 전화번호
                String phone = customerNode.path("phoneNumber").asText();
                // 생년월일 (YYYY-MM-DD)
                String birth = customerNode.path("birthDate").asText();
                
                try {
                	// 이름, 전화번호, 생년월일로 사용자 조회
                	User user = loginService.findIdBySimpleAuth(name, phone, birth);
                    responseBody.put("status", "success");
                    responseBody.put("userId", user.getUserId());

                    return ResponseEntity.ok(responseBody);
                } catch (LoginException le) {
                    // 회원 정보 없음
                    responseBody.put("status", "userNotFound");
                    responseBody.put("message", le.getMessage());
                    return ResponseEntity.ok(responseBody);
				}
                
            } else {
                // 인증 실패
                String reason = root.path("reason").asText("알 수 없는 이유로 인증 실패");
                responseBody.put("status", "failed");
                responseBody.put("message", reason);
                return ResponseEntity.badRequest().body(responseBody);
            }

        } catch (Exception e) { 
            e.printStackTrace();
            responseBody.put("status", "error");
            responseBody.put("message", "인증 처리 중 서버 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(responseBody);
        }
    }
	
	// 비밀번호 찾기 페이지로 이동
	@GetMapping("/findPw")
	public String findPwCheckForm(Model model) {
        model.addAttribute("portoneStoreId", portoneStoreId);
        model.addAttribute("portoneChannelKey", portoneChannelKey);
        model.addAttribute("portoneApiSecret", portoneApiSecret);
        
		return "user/findPasswordForm";
	}
	
	@PostMapping("/findPw")
	@ResponseBody
    public ResponseEntity<?> findPwCheck(@RequestParam String userId) {
        
        try {
        	User user = loginService.findPwById(userId);
        	
        	return ResponseEntity.ok(user.getUserId());
        } catch (LoginException le) {
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(le.getMessage());
		}
        
    }

	// 비밀번호 변경
    @PostMapping("/changePassword")
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
