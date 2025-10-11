package com.bookat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.dto.myPage.ReservationDetailDto;
import com.bookat.dto.myPage.TicketDetailDto;
import com.bookat.entity.User;
import com.bookat.service.MyPageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/myPage")
@RequiredArgsConstructor
public class MyPageController {
	
	private final MyPageService myPageService;
	
	@GetMapping("/")
	public String myPage(@AuthenticationPrincipal User user) {
		return "mypage/myPageMain";
	}

	@GetMapping("/orderList")
	public String orderListPage() {
		return "forward:/order/orderList";
	}
	
	// [예매 내역 관련]
	// ===========================================================================================
	// 예매 내역 조회
	@GetMapping("/reservationDetails")
	public ResponseEntity<Map<String, Object>> reservationDetails(@AuthenticationPrincipal User user) {
		List<ReservationDetailDto> reservations =  myPageService.getReservationDetails(user.getUserId());
		
		if(reservations == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "예약 내역 조회에 실패하였습니다."));
		}
		
		return ResponseEntity.ok(Map.of("status", HttpStatus.OK.value(), "reservations", reservations));
	}
	
	// 예매 내역별 티켓내역 조회
	@GetMapping("/ticketDetails")
	public ResponseEntity<Map<String, Object>> ticketDetails(@RequestParam int reservationId) {
		List<TicketDetailDto> tickets = myPageService.getTicketDetails(reservationId);
		
		if(tickets == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "티켓 정보 조회에 실패하였습니다."));
		}
		
		return ResponseEntity.ok(Map.of("status", HttpStatus.OK, "ticketType", tickets.get(0).getTicketType(), "tickets", tickets));
	}
	
	// [개인 정보 수정]
	// ===========================================================================================

}
