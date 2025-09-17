package com.bookat.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.entity.Event;
import com.bookat.entity.Payment;
import com.bookat.entity.User;
import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.EventPart;
import com.bookat.entity.reservation.SeatType;
import com.bookat.entity.reservation.Ticket;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	
	// 임시 티켓팅 팝업 오픈
	@GetMapping("/")
	public String reservation(@RequestParam int eventId, @AuthenticationPrincipal User user, Model model) {
		
		/*
		 * 알고있어야 하는 정보
		 * 
		 * eventId, scheduleId
		 * 
		 * 좌석 및 선착순 잔여수량
		 * 
		 * */
		Event event = reservationService.startReservation(eventId);
		
		// 이벤트 티켓 타입 저장
		Ticket ticket = new Ticket();
		ticket.setTicketType(event.getTicketType());
		
		// 예약하는 유저 저장
		Reservation reservation = new Reservation();
		reservation.setUserId(user.getUserId());
		log.info("예약 컨트롤러 유저아이디 : {}", reservation.getUserId());
		
		// 이벤트 회차에 해당 이벤트 아이디 저장
		EventPart eventPart = new EventPart();
		eventPart.setEventId(event.getEventId());
		log.info("예약 컨트롤러 이벤트아이디 : {}", eventPart.getEventId());
		
		// 좌석 유형에 해당 이벤트 아이디 저장
		SeatType seatType = new SeatType();
		seatType.setEventId(event.getEventId());

		model.addAttribute("event", event);
		model.addAttribute("ticket", ticket);
		model.addAttribute("seatType", seatType);
		model.addAttribute("schedule", eventPart);
		model.addAttribute("payment", new Payment());
		model.addAttribute("reservation", new Reservation());

		return "reservation/ReservationPopup_Person";
	}
	
	// 인원 등급 선택
	@PostMapping("/eventSchedule")
	public String eventSchedule(@AuthenticationPrincipal User user) {
		boolean existUser = userIsNotNull(user);
		
		/*
		 * 요청
		 * 날짜, 회차
		 * 
		 * 응답
		 * 해당 회차의 잔여수량 ...
		 * 
		 * */
		return "";
	}
	
	// 인원 등급 선택
	@PostMapping("/personTypeInfo")
	public ResponseEntity<?> personTypeInfo(@AuthenticationPrincipal User user) {
		boolean existUser = userIsNotNull(user);
		
		/*
		 * 요청
		 * 인원 등급 별 인원 수, 최종 금액
		 * 
		 * 응답
		 * 최종 금액 ...
		 * 
		 * */
		return ResponseEntity.ok(Map.of("", ""));
	}
	
	// 사용자가 존재하면 true, null 이면 false
	private boolean userIsNotNull(User user) {
		if(user != null) {
			return true;
		}
		return false;
	}
	
}
