package com.bookat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.service.QueueService;

// 대기열 컨트롤러
@Controller
@RequestMapping("/queue")
public class QueueController {
	
	@Autowired
	private QueueService queueService;
	
	// 테스트를 위한 메인 페이지 
	@GetMapping("/test")
	public String queue(Model model) {
		return "reservation/QueueModal";
	}
	
	// 대기열 진입 API (예매하기 버튼 클릭시) 
	@PostMapping("/enter")
	public ResponseEntity<Map<String, Object>> enterQueue(@RequestParam String eventId, @RequestParam String userId){
				
		// userId는 클라이언트에서 전달받은 랜덤 값 사용
	    if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
	        return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "eventId/userId 필요"));
	    }
		
		Long rank = queueService.addUserToQueue(eventId, userId);
		
		return ResponseEntity.ok(Map.of(
				"status", "success", 
				"rank", rank != null? rank : 0L, 
				"userId", userId)
		);
	}
	
	// 대기열 상태 확인 API 
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getQueueStatus(@RequestParam String eventId, @RequestParam String userId){
		Long rank = queueService.getUserRank(eventId, userId);
		
		return ResponseEntity.ok(Map.of(
				"status", "success", 
				"rank", rank!=null? rank : null)
		);
	}
	
	// 대기열에서 삭제 & 예매팝업으로 이동 API 
	@PostMapping("/leave")
	public ResponseEntity<Map<String, Object>> leaveQueue(
			@RequestParam("eventId") String eventId, 
			@RequestParam("userId") String userId){
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			boolean removed = queueService.leaveQueue(eventId, userId);
			
			if(removed) {
				response.put("status", "success");
				response.put("message", "대기열에서 제거되었습니다.");
			} else {
				response.put("status", "fail");
				response.put("message", "대기열에서 사용자를 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			response.put("status", "error");
			response.put("message", "대기열 제거 중 오류 발생 : "+e.getMessage());
		}
		
		return ResponseEntity.ok(response);
	}
	
	// 임시 티켓팅 팝업 오픈 --> 이후 ReservationController로 이동 
	@GetMapping("/reservation")
	public String reservation() {
		return "reservation/ReservationPopup";
	}
	
}
