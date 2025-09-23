package com.bookat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.dto.EventSeatInfoDto;
import com.bookat.dto.SeatValidationRequestDto;
import com.bookat.service.SeatService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservation/seat2")
public class SeatController {
	
	@Autowired
	private SeatService seatService;
	
	// 좌석 목록 조회
	@GetMapping("/getSeats")
	public ResponseEntity<List<EventSeatInfoDto>> getSeats(
			@RequestParam("eventId") int eventId, 
			@RequestParam("scheduleId") int scheduleId){
		
		List<EventSeatInfoDto> seatList = seatService.getSeatList(eventId, scheduleId);
		return ResponseEntity.ok(seatList);
	}
	
	// 좌석 유효성 검증 
	@PostMapping("/validateSeats")
	public ResponseEntity<?> validateSelectedSeats(@RequestBody SeatValidationRequestDto request) {
		log.info("좌석 유효성 검증 요청 : {}", request);
		
		// 선택한 모든 좌석이 유효한 상태인지 확인
		boolean allAvailable = seatService.checkAllSeatsAvailable(
				request.getEventId(), 
				request.getScheduleId(), 
				request.getSeatNames());
		
		if(!allAvailable) {
			log.warn("일부 좌석이 이미 선택되었습니다.");
			return ResponseEntity
					.status(HttpStatus.CONFLICT)	// 409 conflict 상태 코드 
					.body("{\"message\": \"다른 고객님께서 이미 선택한 좌석입니다.\"}");
		}
		
		// 모든 좌석이 유효하다면, 해당 좌석들을 HOLD 상태로 변경 (선점) 
		boolean allHold = seatService.holdSeats(
				request.getEventId(),
				request.getScheduleId(), 
				request.getSeatNames());
		
		if(allHold) {
			log.info("좌석 선점 성공");
			return ResponseEntity.ok().build();
		} else {
			log.error("좌석 선점 실패");
			return ResponseEntity
					.status(HttpStatus.CONFLICT)	// 409 conflict 상태 코드 
					.body("{\"message\": \"좌석 선점 과정에서 오류가 발생했습니다. 다시 시도해 주세요.\"}");
		}
	}
	
}
