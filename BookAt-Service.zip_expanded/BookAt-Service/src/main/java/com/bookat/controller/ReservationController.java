package com.bookat.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.EventSeatInfoDto;
import com.bookat.dto.PaymentDto;
import com.bookat.dto.reservation.PaymentInfoResDto;
import com.bookat.dto.reservation.PaymentReservationSession;
import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.SeatTypeReqDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.User;
import com.bookat.enums.PersonType;
import com.bookat.service.PaymentService;
import com.bookat.service.QueueService;
import com.bookat.service.ReservationService;
import com.bookat.service.SeatService;
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
	private final SeatService seatService;
	private final QueueService queueService;
	private final PaymentSessionStore paymentSessionStore;
	
	// 티켓팅 팝업 오픈
	@GetMapping("/start")
	public String reservation(@RequestParam int eventId, @AuthenticationPrincipal User user, Model model) {

		if (user == null) {
			throw new RuntimeException("예약 가능 유저가 없습니다.");
		}

		ReservationStartDto reservationStartDto = reservationService.startReservation(eventId, user.getUserId());
		model.addAttribute("event", reservationStartDto.getEvent());
		model.addAttribute("eventParts", reservationStartDto.getEventParts());
		model.addAttribute("reservationToken", reservationStartDto.getReservationToken());

		return "reservation/ReservationPopup";
	}

	// step1: 날짜/회차 선택
	@PostMapping("/{reservationToken}/step1")
	public ResponseEntity<Map<String, Object>> selectSchedule(@PathVariable String reservationToken,
			@RequestBody Map<String, Object> request) {

		int scheduleId = (Integer) request.get("scheduleId");
		reservationService.selectSchedule(reservationToken, scheduleId);

		Map<String, Object> response = new HashMap<>();
		response.put("message", "회차 선택 완료");
		response.put("status", "STEP2");
		response.put("scheduleId", scheduleId);

		return ResponseEntity.ok(response);
	}
	
	// step2 : 인원/좌석 선택 (티켓 유형별 처리) 
	@PostMapping("/{reservationToken}/step2")
	public ResponseEntity<Map<String, Object>> selectPersonType(@PathVariable String reservationToken,
																@RequestBody Map<String, Object> payload) {

		try {
			String ticketType = (String) payload.get("ticketType");
			log.info("STEP2 요청 ticketType = {}", ticketType);
			Map<String, Object> response = new HashMap<>();
			
			if("PERSON_TYPE".equals(ticketType)) {
				@SuppressWarnings("unchecked")
				Map<String, Integer> countsMap = (Map<String, Integer>) payload.get("personCounts");
				
				if (countsMap == null || countsMap.isEmpty()) {
			        return ResponseEntity.badRequest()
			                .body(Map.of("status", "STEP2", "error", "선택된 인원이 없습니다."));
			    }
				
				// 문자열 key -> PersonType enum 변환
	            Map<PersonType, Integer> personCounts = countsMap.entrySet().stream()
	                    .collect(Collectors.toMap(
	                            e -> PersonType.valueOf(e.getKey()), // "ADULT" -> PersonType.ADULT
	                            Map.Entry::getValue
	                    ));
				
	            int totalPrice = (int) payload.getOrDefault("totalPrice", 0);
	            
	            PersonTypeReqDto dto = new PersonTypeReqDto(personCounts, totalPrice);
	            
				// 인원 선택 유형 처리 
				reservationService.selectPersonType(reservationToken, dto);
				
				response.put("message", "인원 선택 완료");
	            response.put("totalPrice", totalPrice);
				
			} else if ("SEAT_TYPE".equals(ticketType)) {
				// 좌석 선택 유형 처리 
				int eventId = (Integer) payload.get("eventId");
				int scheduleId = (Integer) payload.get("scheduleId");
				@SuppressWarnings("unchecked")
				List<String> seatNames = (List<String>) payload.get("seatNames");
				int totalPrice = (Integer) payload.getOrDefault("totalPrice", seatNames.size() * 10000);
				
				SeatTypeReqDto seatDto = new SeatTypeReqDto(eventId, scheduleId, seatNames, totalPrice);
				
				reservationService.selectSeatType(reservationToken, seatDto);
				
				response.put("message", "좌석 선택 완료");
	            response.put("totalPrice", totalPrice);
				
			} else {
				return ResponseEntity.badRequest()
	                    .body(Map.of("status", "STEP2", "error", "티켓 타입이 올바르지 않습니다."));
			}
			
			response.put("status", "STEP3");			
			return ResponseEntity.ok(response);
			
		} catch (IllegalArgumentException iae) {
			// 좌석 부족 오류 발생
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "STEP2");
			errorResponse.put("error", iae.getMessage());

			// 409 에러 : 충돌 상황 -> 잔여좌석과 선택좌석의 충돌
			return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
		} catch (Exception e) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("status", "STEP2");
			errorResponse.put("error", "알 수 없는 오류가 발생했습니다.");

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}

	}

	// step3 : 사용자 정보 입력
	@PostMapping("/{reservationToken}/step3")
	public ResponseEntity<Map<String, Object>> inputUserInfo(@PathVariable String reservationToken,
			@AuthenticationPrincipal User user, @RequestBody UserInfoReqDto userInfoReqDto) {
		try {
			boolean success = reservationService.inputUserInfo(reservationToken, user.getUserId(), userInfoReqDto);
			if(!success) {
				log.warn("STEP3 abort: 예약 정보 저장 실패");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "예약 정보 저장 실패"));
			}
			
			// 결제 프레그먼트 연결
			PaymentInfoResDto getPaymentInfo = reservationService.getPaymentInfo(reservationToken);
			
			String enforcedMethod = "CARD";
			PaymentDto pay = paymentService.createReadyPayment(getPaymentInfo.getTotalPrice(), enforcedMethod, getPaymentInfo.getTitle(), user.getUserId(),null);

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
			log.info("STEP3 completed successfully: paymentStepUrl={}", paymentStepUrl);
			
			return ResponseEntity.ok(Map.of("message", "사용자 정보 저장 완료", "status", "STEP4", "paymentStepUrl", paymentStepUrl));
			
		} catch (IllegalStateException ise) {
			log.error("STEP3 IllegalStateException: {}", ise.getMessage(), ise);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "STEP3", "error", ise.getMessage()));
		} catch (Exception e) {
			log.error("STEP3 Exception: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "STEP3", "error", "서버 오류가 발생했습니다."));
		}
	}

	// 예약 확인 : 토큰 유효한지 확인 
	@GetMapping("/{reservationToken}/check")
	public ResponseEntity<Map<String, Object>> checkReservation(@PathVariable String reservationToken) {
		try {
			reservationService.validateReservation(reservationToken);

			return ResponseEntity.ok(Map.of("valid", true));
		} catch (IllegalStateException ie) {

			return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", ie.getMessage()));
		}
	}
	
	// 좌석 취소 : 팝업닫기, 예약 세션 만료, 결제취소(결제 전 브라우저종료 or step4에서 이전단계로 이동)
	@PostMapping("/{reservationToken}/cancel")
	public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable String reservationToken, @RequestBody Map<String, Object> body) {

		boolean isPaymentStep = Boolean.parseBoolean(String.valueOf(body.getOrDefault("isPaymentStep", "false")));
		String reason = String.valueOf(body.getOrDefault("reason", null));
		
		try {
			reservationService.cancelReservation(reservationToken, isPaymentStep, reason);

			return ResponseEntity.ok(Map.of("message", "예약이 성공적으로 취소되었습니다.", "status", "CANCEL"));
		} catch (IllegalStateException ie) {
			log.warn("예약 세션 만료 : {}", ie);

			// 410 에러: 리소스가 영구적으로 사라졌다는 의미
			return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", ie.getMessage(), "status", "EXPIRED"));
		} catch (Exception e) {
			log.error("예약 취소 실패 : {}", e);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "예약 취소 중 오류 발생"));
		}
	}

	// =========================================================================================================

	// [좌석유형] 결제 완료 후 예약 확정 (좌석 상태 변경)
	@PostMapping("/{reservationToken}/confirmBooking")
	public ResponseEntity<String> confirmBooking(@PathVariable String reservationToken, @RequestBody SeatTypeReqDto reqDto){
		try {
			reservationService.confirmBooking(reservationToken, reqDto);
			return ResponseEntity.ok("좌석 예약이 확정되었습니다.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("예약 확정 중 오류 발생: " + e.getMessage());
		}
	}
	
	// =========================================================================================================
	
	// 예매 완료 버튼 누르면 호출 (submit) -> 결제성공이 완료된 시점
	@PostMapping("/complete")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> completeReservation(@RequestBody Map<String, String> request, @AuthenticationPrincipal(expression = "userId") String userId) {

		String reservationToken = request.get("token");
		
		if(reservationToken == null || userId == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "잘못된 요청입니다.", "status", "INVALID_REQUEST"));
		}
		
		try {
			// redis 예매 세션 삭제, 좌석 복구X, active set에서 user 삭제
			reservationService.completeReservation(reservationToken, userId);
			
			return ResponseEntity.ok(Map.of("message", "예매가 성공적으로 완료되었습니다.", "status", "SUCCESS"));
			
		} catch(Exception e) {
			log.error("예매 완료 실패 : {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "예매 완료 처리 중 오류 발생", "status", "ERROR"));
		}
	}
	
	// =========================================================================================================
	
	// 좌석 조회 API 
	@GetMapping("/seat/getSeats")
	public ResponseEntity<List<EventSeatInfoDto>> getSeats(
	        @RequestParam int eventId,
	        @RequestParam int scheduleId) {

	    List<EventSeatInfoDto> seats = seatService.getSeatList(eventId, scheduleId);
	    return ResponseEntity.ok(seats);
	}
	
	// 선택한 좌석/예약 상태 초기화 
	@PostMapping("/{reservationToken}/reset")
	public ResponseEntity<Map<String, Object>> resetReservation(
				@PathVariable String reservationToken,
				@RequestBody Map<String, Object> payload){
		
		try {
			int eventId = (Integer) payload.get("eventId");
			int scheduleId = (Integer) payload.get("scheduleId");
			
			@SuppressWarnings("unchecked")
			List<String> seatNames = ((List<String>) payload.get("seatNames"))
										.stream().map(Object::toString)
										.collect(Collectors.toList());
			
			if(!seatNames.isEmpty()) {
				seatService.releaseSeats(eventId, scheduleId, seatNames);
			}
			
			return ResponseEntity.ok(Map.of("message", "예약 상태 초기화 완료"));
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
								 .body(Map.of("error", "예약 초기화 중 오류 발생"));
		}
	}
	
}
