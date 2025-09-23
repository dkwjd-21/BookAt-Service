/*package com.bookat.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookat.util.CaptchaStore;
import com.bookat.util.CaptchaUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.captcha.Captcha;

@Slf4j
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {
	
	private final CaptchaStore captchaStore;
	
	// 캡챠 이미지 생성 API 
	@GetMapping("/image")
	public ResponseEntity<?> getCaptchaImage() throws IOException {
		
		String captchaId = UUID.randomUUID().toString();
		Captcha captcha = CaptchaUtil.createCaptcha();
		
		// redis 저장
		captchaStore.save(captchaId, captcha.getAnswer());
		
		byte[] imageBytes = CaptchaUtil.toImageBytes(captcha);
		
		Map<String, Object> response = new HashMap<>();
		response.put("captchaId", captchaId);
		response.put("image", Base64.getEncoder().encodeToString(imageBytes));
		
		return ResponseEntity.ok(response);
	}
	
	// 캡챠 오디오 생성 API 
	@GetMapping("/audio")
	public ResponseEntity<byte[]> getCaptchaAudio(@RequestParam String captchaId) throws IOException {
		String answer = captchaStore.get(captchaId);
		
		if(answer == null) {
			return ResponseEntity.badRequest().build();
		}

		byte[] audioBytes = CaptchaUtil.toAudioBytes(answer);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
								  .body(audioBytes);
	}
	
	// 사용자 입력값 검증 API 
	@PostMapping("/verify")
	public ResponseEntity<Map<String, Boolean>> verifyCaptcha(@RequestParam String captchaId, @RequestParam String answer){
		
		String correctAnswer = captchaStore.get(captchaId);
		boolean success = correctAnswer != null && correctAnswer.equals(answer);
		
		if(success) {
			captchaStore.remove(captchaId);
		}
		
		// 결과를 JSON 형태로 반환 
		Map<String, Boolean> response = Collections.singletonMap("success", success);
		return ResponseEntity.ok(response);
	}
}*/
