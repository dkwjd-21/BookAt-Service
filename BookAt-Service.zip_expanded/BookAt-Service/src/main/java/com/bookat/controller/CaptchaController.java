/*
 * package com.bookat.controller;
 * 
 * import java.io.IOException; import java.util.Collections; import
 * java.util.Map;
 * 
 * import org.springframework.http.MediaType; import
 * org.springframework.http.ResponseEntity; import
 * org.springframework.web.bind.annotation.GetMapping; import
 * org.springframework.web.bind.annotation.PostMapping; import
 * org.springframework.web.bind.annotation.RequestMapping; import
 * org.springframework.web.bind.annotation.RequestParam; import
 * org.springframework.web.bind.annotation.RestController;
 * 
 * import com.bookat.util.CaptchaUtil;
 * 
 * import jakarta.servlet.http.HttpSession; import nl.captcha.Captcha;
 * 
 * @RestController
 * 
 * @RequestMapping("/api/captcha") public class CaptchaController { // 캡챠 이미지 생성
 * API
 * 
 * @GetMapping("/image") public ResponseEntity<byte[]>
 * getCaptchaImage(HttpSession session) throws IOException { byte[] imageBytes =
 * CaptchaUtil.generateImage(session); return
 * ResponseEntity.ok().contentType(MediaType.IMAGE_PNG) .body(imageBytes); }
 * 
 * // 캡챠 오디오 생성 API
 * 
 * @GetMapping("/audio") public ResponseEntity<byte[]>
 * getCaptchaAudio(HttpSession session) throws IOException{ byte[] audioBytes =
 * CaptchaUtil.generateAudio(session); return
 * ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
 * .body(audioBytes); }
 * 
 * // 사용자 입력값 검증 API
 * 
 * @PostMapping("/verify") public ResponseEntity<Map<String, Boolean>>
 * verifyCaptcha(
 * 
 * @RequestParam("answer") String userAnswer, HttpSession session){
 * 
 * Captcha captcha = (Captcha) session.getAttribute(Captcha.NAME); boolean
 * isCorrect = false;
 * 
 * if(captcha != null && userAnswer != null) { isCorrect =
 * captcha.isCorrect(userAnswer); if(isCorrect) { // 성공시 세션에서 캡챠 정보 제거
 * session.removeAttribute(Captcha.NAME); } }
 * 
 * // 결과를 JSON 형태로 반환 Map<String, Boolean> response =
 * Collections.singletonMap("success", isCorrect); return
 * ResponseEntity.ok(response); } }
 */
