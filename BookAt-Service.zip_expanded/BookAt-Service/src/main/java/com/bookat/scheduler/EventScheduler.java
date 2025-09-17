package com.bookat.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.bookat.dto.EventResDto;
import com.bookat.service.impl.EventServiceImpl;

@Component
public class EventScheduler {

	@Autowired
	public EventServiceImpl eventServiceImpl;

	@Autowired
	private StringRedisTemplate redisTemplate;

	// 매일 자정 스케쥴링 - 테스트를 위해 비활성화 상태로 구현
	// @Scheduled(cron = "0 0 0 * * * ")
	public void preloadEventSets() {

	}

	// 테스트용 메서드
	public void testPreloadEventSets() {
		// 1. 내일 예매 오픈 이벤트 조회 
		// 2. PersonType / SeatType 구분 
		// 3. 총 좌석 수 조회 
		// 4. Redis에 좌석 정보 세팅 + TTL
	}

	// 공통 로직
	public void preloadSeatsLogic() {
		
		LocalDate targetEventDate = LocalDate.now().plusDays(31);
		Date targetDate = Date.from(targetEventDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
		
		// 1. DB에서 내일 예매 오픈 이벤트 조회
		List<EventResDto> events = eventServiceImpl.selectByEventDate(targetDate);

		// 2. events 리스트를 순회하며 eventId로 이벤트 회차 테이블 조회 
		// 잔여좌석 값 가져오기 
		
		// 3. events 리스트에서 티켓타입을 확인 후 
		// personType이면 Redis -> 총 좌석수 & 잔여좌석 저장하기  
		// SeatType이면 Redis -> 좌석 상태 저장하기 
	
		
		System.out.println(events);
	}

}
