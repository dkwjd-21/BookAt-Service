package com.bookat.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookat.service.QueueService;

// 대기열 컨트롤러
@RestController
@RequestMapping("/queue")
public class QueueController {
	
	@Autowired
	private QueueService queueService;
	
	// 대기열 진입 API (예매하기 버튼 클릭시) 
	@PostMapping("/enter")
	public ResponseEntity<Map<String, Object>> enterQueue(){
		// 이벤트 아이디 & 유저아이디 : 임의지정
		String eventId = "21";
		String userId = "dkwjd";
		
		Long rank = queueService.addUserToQueue(eventId, userId);
		
		return ResponseEntity.ok(Map.of("status", "success", "rank", rank, "userId", userId));
	}
	
	// 대기열 상태 확인 API 
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getQueueStatus(@RequestParam String eventId, @RequestParam String userId){
		Long rank = queueService.getUserRank(eventId, userId);
		
		return ResponseEntity.ok(Map.of("status", "success", "rank", rank));
	}
}
