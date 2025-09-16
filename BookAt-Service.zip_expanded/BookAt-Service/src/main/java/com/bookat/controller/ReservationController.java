package com.bookat.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.entity.Event;
import com.bookat.entity.Payment;
import com.bookat.entity.User;
import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Schedule;
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
		
		Event event = reservationService.startReservation(eventId);
		
		// 이벤트 티켓 타입 저장
		Ticket ticket = new Ticket();
		ticket.setTicketType(event.getTicketType());
		
		// 예약하는 유저 저장
		Reservation reservation = new Reservation();
		reservation.setUserId(user.getUserId());
		log.info("예약 컨트롤러 유저아이디 : {}", reservation.getUserId());
		
		// 이벤트 회차에 해당 이벤트 아이디 저장
		Schedule schedule = new Schedule();
		schedule.setEventId(event.getEventId());
		log.info("예약 컨트롤러 이벤트아이디 : {}", schedule.getEventId());
		
		// 좌석 유형에 해당 이벤트 아이디 저장
		SeatType seatType = new SeatType();
		seatType.setEventId(event.getEventId());

		model.addAttribute("event", event);
		model.addAttribute("ticket", ticket);
		model.addAttribute("seatType", seatType);
		model.addAttribute("schedule", schedule);
		model.addAttribute("payment", new Payment());
		model.addAttribute("reservation", new Reservation());

		return "reservation/ReservationPopup_Person";
	}
	
}
