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

import com.bookat.entity.User;
import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;
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
	
	// [예매 내역 관련]
	// ===========================================================================================
	// 예매 내역 조회
	@GetMapping("/reservationDetails")
	public ResponseEntity<Map<String, Object>> reservationDetails(@AuthenticationPrincipal User user) {
		List<Reservation> reservations =  myPageService.getReservations(user.getUserId());
		
		return ResponseEntity.ok(Map.of("status", HttpStatus.OK, "reservations", reservations));
	}
	
	// 예매 내역별 티켓내역 조회
	@GetMapping("/ticketDetails")
	public ResponseEntity<Map<String, Object>> ticketDetails(@RequestParam int reservationId) {
		List<Ticket> tickets = myPageService.getTickets(reservationId);
		
		return ResponseEntity.ok(Map.of("status", HttpStatus.OK, "tickets", tickets));
	}
	
	// [개인 정보 수정]
	// ===========================================================================================

}
