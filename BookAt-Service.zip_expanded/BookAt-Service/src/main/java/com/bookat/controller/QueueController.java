package com.bookat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.entity.User;
import com.bookat.service.QueueService;
import lombok.extern.slf4j.Slf4j;

// 대기열 컨트롤러
@Slf4j
@Controller
@RequestMapping("/queue")
public class QueueController {

	@Autowired
	private QueueService queueService;

	// 테스트용 진입 API
	@GetMapping
	public String test() {
		return "reservation/QueueModal";
	}

	// 대기열 진입 API (예매하기 버튼 클릭시)
	@PostMapping("/enter")
	public ResponseEntity<Map<String, Object>> enterQueue(@RequestParam String eventId,
			@AuthenticationPrincipal User user) {

		// userId는 클라이언트에서 전달받은 값 사용
		if (eventId == null || eventId.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "eventId 필요"));
		}

		String userId = user.getUserId();
		Long rank = queueService.addUserToQueue(eventId, userId);

		log.info("eventId : {}", eventId);
		log.info("userId : {}", userId);
		log.info("rank : {}", rank);

		return ResponseEntity.ok(Map.of("status", "success", "rank", rank != null ? rank : 0L, "userId", userId));
	}

	// 대기열 상태 확인 API
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getQueueStatus(@RequestParam String eventId,
			@AuthenticationPrincipal User user) {
		Long rank = queueService.getUserRank(eventId, user.getUserId());
		
		boolean canEnter = queueService.canEnterReservation(eventId, user.getUserId()); 
		
		Map<String, Object> response = new HashMap<>();
		response.put("status", "waiting");
		response.put("rank", rank);
		response.put("canEnter", canEnter);
		response.put("waitingCount", queueService.getQueueCount(eventId));
		
		return ResponseEntity.ok(response);
	}

	// 대기열에서 삭제 & 예매팝업으로 이동 API
	@PostMapping("/leave")
	public ResponseEntity<Map<String, Object>> leaveQueue(@RequestParam("eventId") String eventId,
			@AuthenticationPrincipal User user) {

		Map<String, Object> response = new HashMap<>();

		try {
			String userId = user.getUserId();
            boolean removed = queueService.leaveQueue(eventId, userId);
            queueService.leaveActive(eventId, userId);

			if (removed) {
				response.put("status", "success");
				response.put("message", "대기열에서 제거되었습니다.");
			} else {
				response.put("status", "fail");
				response.put("message", "대기열에서 사용자를 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			response.put("status", "error");
			response.put("message", "대기열 제거 중 오류 발생 : " + e.getMessage());
		}

		return ResponseEntity.ok(response);
	}
	
	// 팝업 열기 성공 후 호출
	@PostMapping("/enterActive")
	public ResponseEntity<Map<String, Object>> enterActive(@RequestParam String eventId,
	        @AuthenticationPrincipal User user) {

	    String userId = user.getUserId();
	    boolean success = queueService.tryEnterActive(eventId, userId);

	    if (success) {
	        return ResponseEntity.ok(Map.of("status", "entered", "message", "예매창으로 이동", "userId", userId));
	    } else {
	        return ResponseEntity.ok(Map.of("status", "fail", "message", "Active 입장 실패", "userId", userId));
	    }
	}

	@PostMapping("/heartbeat")
	public ResponseEntity<Void> hearbeat(@RequestParam String eventId, @AuthenticationPrincipal User user){
		queueService.updateLastActive(eventId, user.getUserId());
	    return ResponseEntity.ok().build();
	}
}
