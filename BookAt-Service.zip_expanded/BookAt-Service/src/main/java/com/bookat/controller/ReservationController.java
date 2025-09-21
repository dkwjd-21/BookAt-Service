package com.bookat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.entity.Event;
import com.bookat.entity.User;
import com.bookat.entity.reservation.EventPart;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

	@Autowired
	private final ReservationService reservationService;
	
	// 임시 티켓팅 팝업 오픈
	@GetMapping("/start")
	public String reservation(@RequestParam int eventId, @AuthenticationPrincipal User user, Model model) {
		
		if(user == null) {
			throw new RuntimeException("예약 가능 유저가 없습니다.");
		}
		
		Event event = reservationService.startReservation(eventId);
		log.info("이벤트 아이디 : {}", event.getEventId());
		log.info("이벤트 좌석 타입 : {}", event.getTicketType());
		
		// eventId로 회차 리스트 조회
		List<EventPart> partList = reservationService.selectPartsByEventId(eventId);
		log.info("회차 리스트 : {}", partList);
		
		model.addAttribute("event", event);
		model.addAttribute("partList", partList);

		// 테스트용으로 ReservationPopup_Seat2.html에서 진행
		return "reservation/ReservationPopup_Seat2";
	}
	
	// 날짜 회차 선택
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
	
	// 주문자 정보 입력
	@PostMapping("/inputUserInfo")
	public ResponseEntity<?> inputUserInfo(@AuthenticationPrincipal User user) {
		boolean existUser = userIsNotNull(user);
		
		/*
		 * 요청
		 * 주문자 이름, 전화번호, 이메일
		 * 
		 * 응답
		 * 사용자 인증 여부?
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
