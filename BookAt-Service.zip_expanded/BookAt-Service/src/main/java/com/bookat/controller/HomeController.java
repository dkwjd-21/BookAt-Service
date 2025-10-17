package com.bookat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
	
	private final ReservationService reservationService;
	
    @GetMapping("/")
    public String root() {
        return "redirect:/books";
    }
	
	// 테스트를 위한 메인 페이지 
	@GetMapping("/infoPage/reservationTest")
	public String queue(Model model) {
		// [좌석유형] 예매번호 122번으로 테스트 
//		String reservationId = "122";
		// [인원유형] 예매번호 102번으로 테스트
		String reservationId = "102";
		try {
			List<Map<String, Object>> existingDataList = reservationService.getExistingReservationData(reservationId);
			System.out.println(existingDataList);
			model.addAttribute("existingDataList", existingDataList);
			
		} catch (Exception e) {
			// 유효하지 않은 예약 ID거나 조회 실패 시 
			log.error("예약 변경 데이터 로드 실패: reservationId={}", reservationId, e);
		}
		
		return "reservation/QueueModal";
	}
}
