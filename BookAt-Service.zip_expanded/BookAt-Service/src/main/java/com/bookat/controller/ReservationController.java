package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.entity.Event;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	
	// 임시 티켓팅 팝업 오픈
	@GetMapping("/")
	public String reservation(@RequestParam int eventId, Model model) {
		
		Event event = reservationService.startReservation(eventId);
		model.addAttribute("event", event);
		
		return "reservation/ReservationPopup_Person";
	}
	
}
