package com.bookat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.User;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	
	// 티켓팅 팝업 오픈
	@GetMapping("/start")
	public String reservation(@RequestParam int eventId, @AuthenticationPrincipal User user, Model model) {
		
		if(user == null) {
			throw new RuntimeException("예약 가능 유저가 없습니다.");
		}

		ReservationStartDto reservationStartDto = reservationService.startReservation(eventId, user.getUserId());
		model.addAttribute("event", reservationStartDto.getEvent());
		model.addAttribute("eventParts", reservationStartDto.getEventParts());
		log.info("eventParts 잔여석 : {}", reservationStartDto.getEventParts().get(0).getRemainingSeat());
		model.addAttribute("reservationToken", reservationStartDto.getReservationToken());

		return "reservation/ReservationPopup";
	}
	
	// step1: 날짜/회차 선택
	@PostMapping("/{reservationToken}/step1")
	public ResponseEntity<Map<String, Object>> selectSchedule(@PathVariable String reservationToken, @RequestBody Map<String, Object> request) {
		
		int scheduleId = (Integer) request.get("scheduleId");
		reservationService.selectSchedule(reservationToken, scheduleId);
		
		Map<String, Object> response = new HashMap<>();
		response.put("message", "회차 선택 완료");
		response.put("status", "STEP2");
		response.put("scheduleId", scheduleId);
		
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/{reservationToken}/step2")
	public ResponseEntity<Map<String, Object>> selectPersonType(@PathVariable String reservationToken, @RequestBody PersonTypeReqDto personTypeReqDto) {
		
		try {
			reservationService.selectPersonType(reservationToken, personTypeReqDto);
			
			Map<String, Object> response = new HashMap<>();
			response.put("message", "인원 등급 선택 완료");
			response.put("status", "STEP3");
			response.put("totalPrice", personTypeReqDto.getTotalPrice());
			
			return ResponseEntity.ok(response);
		} catch(IllegalArgumentException iae) {
			// 좌석 부족 오류 발생
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "STEP2");
			errorResponse.put("error", iae.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
		} catch(Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "STEP2");
			errorResponse.put("error", "알 수 없는 오류가 발생했습니다.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}

	}
	
	@PostMapping("/{reservationToken}/step3")
	public ResponseEntity<Map<String, Object>> inputUserInfo(@PathVariable String reservationToken, @AuthenticationPrincipal User user, @RequestBody UserInfoReqDto userInfoReqDto) {
		
		if(user == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "로그인이 필요합니다."));
		}
		
		boolean success = reservationService.inputUserInfo(reservationToken, user.getUserId(), userInfoReqDto);
		
		if(!success) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "예약 정보 저장 실패"));
		}
		
		return ResponseEntity.ok(Map.of("message", "사용자 정보 저장 완료", "status", "STEP4"));
	}
	
}
