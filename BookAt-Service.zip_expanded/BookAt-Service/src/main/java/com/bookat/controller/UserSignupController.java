package com.bookat.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.bookat.dto.UserSignup;
import com.bookat.service.UserSignupService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/signup")
public class UserSignupController {
	
	// service
	@Autowired
	private UserSignupService service;
	
	// application.properties에서 프론트엔드용 키 값을 주입
    @Value("${portone.public.store-id}")
    private String portoneStoreId;
    @Value("${portone.public.channel-key}")
    private String portoneChannelKey;
	@Value("${portone.api_secret}")
	private String portoneApiSecret;
	
	// 회원가입 페이지로 이동 
	@GetMapping
	public String signupVerification(Model model) {
		model.addAttribute("isLoggedIn", false);
		
		// 모델에 키 값을 담아 HTML로 전달
        model.addAttribute("portoneStoreId", portoneStoreId);
        model.addAttribute("portoneChannelKey", portoneChannelKey);
        
		return "user/UserSignup";
	}
	
	// 인증 정보 받아오기
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

            System.out.println("PortOne 응답 데이터: "+response.getBody());
            
            if ("VERIFIED".equals(status)) {
                // 인증 성공
            	JsonNode customerNode = root.path("verifiedCustomer");
                String name = customerNode.path("name").asText();
                // 숫자로만 구성된 전화번호
                String phone = customerNode.path("phoneNumber").asText();
                // 생년월일 (YYYY-MM-DD)
                String birth = customerNode.path("birthDate").asText();

                responseBody.put("status", "success");
                responseBody.put("name", name);
                responseBody.put("phone", phone);
                responseBody.put("birth", birth);
                
                // 세션에 인증 완료 상태 및 정보를 저장
                session.setAttribute("isVerified", true);
                session.setAttribute("verifiedUserName", name);
                session.setAttribute("verifiedUserPhone", phone);
                session.setAttribute("verifiedUserBirth", birth);
                
                return ResponseEntity.ok(responseBody);
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
	
	// 회원 Insert 하기
	@PostMapping("/insert")
	public ResponseEntity<Map<String, Object>> signupInsert(@RequestBody UserSignup input) {
		System.out.println(input.toString());
		
		int res = service.insertUser(input);
		
		Map<String, Object> responseBody = new HashMap<>();
		
		if(res > 0) {
			responseBody.put("message", "회원가입 성공");
			responseBody.put("userName", input.getUserName());
			return ResponseEntity.ok(responseBody);
		} else {
			// 회원가입 실패
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "회원가입에 실패했습니다. 다시 시도해 주세요."));
		}
	}
	
	// 아이디 중복검사 
	@GetMapping("/chkId")
	public ResponseEntity<Boolean> checkId(String idVal) {
		// DB에서 아이디 중복 여부 검사
		boolean isIdAvailable;
		UserSignup user = service.getUserById(idVal);
		System.out.println("[ID] 유저 셀렉트 결과 : "+user);
		
		if(user == null) {
			isIdAvailable = true;
		} else {
			isIdAvailable = false;
		}		
		
		return ResponseEntity.ok(isIdAvailable);
	}
	
	// 이메일 중복검사 
	@GetMapping("/chkEmail")
	public ResponseEntity<Boolean> checkEmail(String emailVal) {
		// DB에서 아이디 중복 여부 검사
		boolean isEmailAvailable;
		UserSignup user = service.getUserByEmail(emailVal);
		System.out.println("[EMAIL] 유저 셀렉트 결과 : "+user);
		
		if(user == null) {
			isEmailAvailable = true;
		} else {
			isEmailAvailable = false;
		}	
				
		return ResponseEntity.ok(isEmailAvailable);
	}
}
