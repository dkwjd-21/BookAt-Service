package com.bookat.controller;

import java.math.BigDecimal;
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

import com.bookat.dto.PaymentDto;
import com.bookat.dto.reservation.PaymentInfoResDto;
import com.bookat.dto.reservation.PaymentReservationSession;
import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.User;
import com.bookat.service.PaymentService;
import com.bookat.service.ReservationService;
import com.bookat.util.PaymentSessionStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	private final PaymentService paymentService;
	private final PaymentSessionStore paymentSessionStore;
	
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
//		return "reservation/ReservationPopup_Seat";
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
			
			// 409 에러 :  충돌 상황 -> 잔여좌석과 선택좌석의 충돌
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
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
		
		try {
			boolean success = reservationService.inputUserInfo(reservationToken, user.getUserId(), userInfoReqDto);
			
			if(!success) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "예약 정보 저장 실패"));
			}
			
			// 결제 프레그먼트 연결
			PaymentInfoResDto getPaymentInfo = reservationService.getPaymentInfo(reservationToken);

			String enforcedMethod = "CARD";
			PaymentDto pay = paymentService.createReadyPayment(getPaymentInfo.getTotalPrice(), enforcedMethod, "pay for event ticket", user.getUserId());
			
			PaymentReservationSession session = PaymentSessionStore.of(
					reservationToken, 
					getPaymentInfo.getEventId(),
					getPaymentInfo.getScheduleId(),
					getPaymentInfo.getTitle(),
					getPaymentInfo.getReservedCount(),
					enforcedMethod,
					BigDecimal.valueOf(getPaymentInfo.getTotalPrice()),
					pay.getMerchantUid(),
					user.getUserId());
			
			String paymentToken =  paymentSessionStore.createEventPay(session);
		      
			String paymentStepUrl = "/payment/" + paymentToken + "/paymentUI";
			
			return ResponseEntity.ok(Map.of("message", "사용자 정보 저장 완료", "status", "STEP4", "paymentStepUrl", paymentStepUrl));
			
		} catch (IllegalStateException ise) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "STEP3", "error", ise.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "STEP3", "error", "서버 오류가 발생했습니다."));
		}

	}
	
	@GetMapping("/{reservationToken}/check")
	public ResponseEntity<Map<String, Object>> checkReservation(@PathVariable String reservationToken) {
		try {
			reservationService.validateReservation(reservationToken);
			
			return ResponseEntity.ok(Map.of("valid", true));
		} catch(IllegalStateException ie) {
			
			return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", ie.getMessage()));
		}
	}
	
	// 좌석 취소 : 팝업닫기, 예약 세션 만료, 결제취소(결제 구현 후에 추가 예정) 등
	@PostMapping("/{reservationToken}/cancel")
	public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable String reservationToken, @RequestBody(required = false) Map<String, Object> body) {
		
		try {
			reservationService.cancelReservation(reservationToken);
			
			return ResponseEntity.ok(Map.of("message", "예약이 성공적으로 취소되었습니다.", "status", "CANCEL"));
		} catch(IllegalStateException ie) {
			log.warn("예약 세션 만료 : {}", ie);
			
			// 410 에러: 리소스가 영구적으로 사라졌다는 의미
			return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", ie.getMessage(), "status", "EXPIRED"));
		} catch(Exception e) {
			log.error("예약 취소 실패 : {}", e);
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "예약 취소 중 오류 발생"));
		}
	}
	
	
	// =========================================================================================================
	
	// 결제 완료 후 reservation 1건 + ticket N건을 생성
	
}
