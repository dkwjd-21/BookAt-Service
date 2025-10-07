package com.bookat.controller;

import java.util.List;
import java.util.Map;

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
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {
	
	private final MyPageService myPageService;
	
	@GetMapping("/reservationDetails")
	public ResponseEntity<Map<String, Object>> reservationDetails(@AuthenticationPrincipal User user) {
		List<Reservation> reservations =  myPageService.getReservations(user.getUserId());
		
		return null;
	}
	
	@GetMapping("/ticketDetails")
	public ResponseEntity<Map<String, Object>> ticketDetails(@RequestParam int reservationId) {
		List<Ticket> tickets = myPageService.getTickets(reservationId);
		
		return null;
	}

}
