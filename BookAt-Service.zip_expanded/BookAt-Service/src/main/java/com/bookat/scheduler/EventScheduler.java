package com.bookat.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import com.bookat.controller.BookController;
import com.bookat.dto.EventPartDto;
import com.bookat.dto.EventResDto;
import com.bookat.dto.EventSeatDto;
import com.bookat.service.impl.BookServiceImpl;
import com.bookat.service.impl.EventServiceImpl;

@Component
//@Controller
public class EventScheduler {

	private final BookServiceImpl bookServiceImpl;

	private final BookController bookController;

	@Autowired
	public EventServiceImpl eventServiceImpl;

	@Autowired
	private StringRedisTemplate redisTemplate;

	EventScheduler(BookController bookController, BookServiceImpl bookServiceImpl) {
		this.bookController = bookController;
		this.bookServiceImpl = bookServiceImpl;
	}

	private int runCount = 0;

	// 매일 자정 스케쥴링 - 테스트를 위해 비활성화 상태로 구현
	@Scheduled(cron = "0 0 0 * * * ")
	// 3분마다 실행, 앱 시작 직후 바로 실행
//	@Scheduled(fixedRate = 180_000, initialDelay = 0)
	public void preloadEventSets() {
		// 3분 단위로 스케쥴링 2회 테스트
		/*
		 * if(runCount >= 2) { System.out.println("테스트 스케쥴링 2회 완료!"); return ; }
		 * 
		 * System.out.println("스케쥴링 preloadSeatsLogic 실행 : "+ (runCount+1) + "번째");
		 * 
		 * 
		 * if(runCount == 0) { // 첫번째 스케쥴링 때 LocalDate targetToday =
		 * LocalDate.now().plusDays(31); preloadSeatsLogic(targetToday); runCount++; }
		 * else if(runCount == 1) { // 두번째 스케쥴링 때 LocalDate targetTomorrow =
		 * LocalDate.now().plusDays(32); preloadSeatsLogic(targetTomorrow); runCount++;
		 * }
		 */

		// 실제 스케쥴링 코드
		LocalDate targetEventDate = LocalDate.now().plusDays(31);	// 이벤트 당일
		preloadSeatsLogic(targetEventDate);
	}

	// 공통 로직
	public ResponseEntity<String> preloadSeatsLogic(LocalDate targetEventDate) {
		long startTime = System.currentTimeMillis();

		try {
			System.out.println("preloadSeatsLogic 실행!");

			// 이벤트 당일
			Date targetDate = Date.from(targetEventDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

			// Redis TTL 설정을 위한 만료일 : 이벤트 다음날 0시
			LocalDate expireDate = targetEventDate.plusDays(1);
			Date expireAt = Date.from(expireDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

			// 1. DB에서 내일 예매 오픈 이벤트 조회
			List<EventResDto> events = eventServiceImpl.selectByEventDate(targetDate);
			System.out.println("events : " + events);

			// 2. events 리스트를 순회하며 eventId로 이벤트 회차 테이블 조회
			// 잔여좌석 값 가져오기

			for (EventResDto event : events) {
				int eventId = event.getEventId();
				String ticketType = event.getTicketType();

				// 이벤트 회차 조회
				List<EventPartDto> parts = eventServiceImpl.selectPartByEventId(eventId);

				System.out.println("eventId : " + eventId);

				if ("SEAT_TYPE".equals(ticketType)) {
					// 좌석 선택 유형의 티켓팅
					for (EventPartDto part : parts) {
						// 회차번호 & 좌석 수
						int scheduleId = part.getScheduleId();
						int remainingSeat = part.getRemainingSeat();
						System.out.println("scheduleId : " + scheduleId + " remainingSeat:" + remainingSeat);

						if (remainingSeat <= 0)
							continue;

						// 총 좌석 열 - (규칙) 총 좌석 수는 무조건 10의 배수로 지정
						int rows = remainingSeat / 10;

						// Redis 키 (Hash, Set)
						String hashKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";
						String availableSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEATS";

						for (int row = 0; row < rows; row++) {
							// A, B, C열 ...
							char rowChar = (char) ('A' + row);

							for (int col = 1; col <= 10; col++) {
								// A1, A2, A3 ...
								String seatName = rowChar + String.valueOf(col);

								// 좌석 테이블 Insert
								EventSeatDto dto = new EventSeatDto();
								dto.setSeatName(seatName);
								dto.setSeatStatus(1);
								dto.setSeatGradeType("STANDARD");
								dto.setEventId(eventId);
								dto.setScheduleId(scheduleId);

								eventServiceImpl.insertSeat(dto);

								// Redis에 좌석 상태 저장 (HashSet)
								redisTemplate.opsForHash().put(hashKey, seatName, "AVAILABLE");
								// Redis에 좌석 상태 저장 (Set)
								redisTemplate.opsForSet().add(availableSetKey, seatName);
							}
						}

						// 키 단위로 한 번만 TTL(만료일) 설정
						redisTemplate.expireAt(hashKey, expireAt);
						redisTemplate.expireAt(availableSetKey, expireAt);
					}

				} else if ("PERSON_TYPE".equals(ticketType)) {
					// 인원 선택 유형의 티켓팅
					for (EventPartDto part : parts) {
						// 회차번호 & 좌석 수
						int scheduleId = part.getScheduleId();
						int remainingSeat = part.getRemainingSeat();

						String totalKey = "event:" + eventId + ":schedule:" + scheduleId + ":TOTAL_SEAT";
						String availKey = "event:" + eventId + ":schedule:" + scheduleId + ":AVAILABLE_SEAT";

						// Redis에 좌석 수 & TTL(만료시간) 저장
						redisTemplate.opsForValue().set(totalKey, String.valueOf(remainingSeat));
						redisTemplate.opsForValue().set(availKey, String.valueOf(remainingSeat));

						redisTemplate.expireAt(totalKey, expireAt);
						redisTemplate.expireAt(availKey, expireAt);

					}
				}

			}

			// 3. events 리스트에서 티켓타입을 확인 후
			// personType이면 Redis -> 총 좌석수 & 잔여좌석 저장하기
			// SeatType이면 Redis -> 좌석 상태 저장하기

			System.out.println(events);
			long endTime = System.currentTimeMillis();
			System.out.println("총 실행 시간 : " + (endTime - startTime) + "ms");

			return ResponseEntity.ok("Preload 완료!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
		}

	}

	// 공통 로직
	@Transactional
	@PostMapping("/test/preloadSeats")
	public ResponseEntity<String> preloadSeatsLogic2() {
		long startTime = System.currentTimeMillis();

		try {
			System.out.println("preloadSeatsLogic 실행!");

			// 이벤트 당일
//			LocalDate targetEventDate = LocalDate.now().plusDays(31);
//			Date targetDate = Date.from(targetEventDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

			// Redis TTL 설정을 위한 만료일 : 이벤트 다음날 0시
//			LocalDate expireDate = targetEventDate.plusDays(1);
//			Date expireAt = Date.from(expireDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

			// 1. DB에서 내일 예매 오픈 이벤트 조회
//			List<EventResDto> events = eventServiceImpl.selectByEventDate(targetDate);
//			System.out.println("events : " + events);

			// 59번 이벤트로 테스트 진행
			EventResDto event = eventServiceImpl.selectOne(59);

			// 2. events 리스트를 순회하며 eventId로 이벤트 회차 테이블 조회
			// 잔여좌석 값 가져오기

			// for (EventResDto event : events) {
			int eventId = event.getEventId();
			String ticketType = event.getTicketType();

			// 이벤트 회차 조회
			List<EventPartDto> parts = eventServiceImpl.selectPartByEventId(eventId);

			System.out.println("eventId : " + eventId);

			if ("SEAT_TYPE".equals(ticketType)) {
				// 좌석 선택 유형의 티켓팅
				for (EventPartDto part : parts) {
					// 회차번호 & 좌석 수
					int scheduleId = part.getScheduleId();
					int remainingSeat = part.getRemainingSeat();
					System.out.println("scheduleId : " + scheduleId + " remainingSeat:" + remainingSeat);

					if (remainingSeat <= 0)
						continue;

					// 총 좌석 열 - (규칙) 총 좌석 수는 무조건 10의 배수로 지정
					int rows = remainingSeat / 10;

					// Redis 키 (Hash, Set)
					String hashKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";
					String availableSetKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEATS";

					for (int row = 0; row < rows; row++) {
						// A, B, C열 ...
						char rowChar = (char) ('A' + row);

						for (int col = 1; col <= 10; col++) {
							// A1, A2, A3 ...
							String seatName = rowChar + String.valueOf(col);

							// 좌석 테이블 Insert
							EventSeatDto dto = new EventSeatDto();
							dto.setSeatName(seatName);
							dto.setSeatStatus(1);
							dto.setSeatGradeType("STANDARD");
							dto.setEventId(eventId);
							dto.setScheduleId(scheduleId);

							eventServiceImpl.insertSeat(dto);

							// Redis에 좌석 상태 저장 (HashSet)
							redisTemplate.opsForHash().put(hashKey, seatName, "AVAILABLE");
							// Redis에 좌석 상태 저장 (Set)
							redisTemplate.opsForSet().add(availableSetKey, seatName);
						}
					}

					// 키 단위로 한 번만 TTL(만료일) 설정
//						redisTemplate.expireAt(hashKey, expireAt);
//						redisTemplate.expireAt(availableSetKey, expireAt);
				}

			} else if ("PERSON_TYPE".equals(ticketType)) {
				// 인원 선택 유형의 티켓팅
				for (EventPartDto part : parts) {
					// 회차번호 & 좌석 수
					int scheduleId = part.getScheduleId();
					int remainingSeat = part.getRemainingSeat();

					String totalKey = "event:" + eventId + ":schedule:" + scheduleId + ":TOTAL_SEAT";
					String availKey = "event:" + eventId + ":schedule:" + scheduleId + ":AVAILABLE_SEAT";

					// Redis에 좌석 수 & TTL(만료시간) 저장
					redisTemplate.opsForValue().set(totalKey, String.valueOf(remainingSeat));
					redisTemplate.opsForValue().set(availKey, String.valueOf(remainingSeat));

//						redisTemplate.expireAt(totalKey, expireAt);
//						redisTemplate.expireAt(availKey, expireAt);

				}
			}

			// }

			// 3. events 리스트에서 티켓타입을 확인 후
			// personType이면 Redis -> 총 좌석수 & 잔여좌석 저장하기
			// SeatType이면 Redis -> 좌석 상태 저장하기

			System.out.println(event);
			long endTime = System.currentTimeMillis();
			System.out.println("총 실행 시간 : " + (endTime - startTime) + "ms");

			return ResponseEntity.ok("Preload 완료!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
		}

	}
}
