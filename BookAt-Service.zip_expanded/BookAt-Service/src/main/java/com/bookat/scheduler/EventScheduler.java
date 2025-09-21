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

import com.bookat.dto.EventPartDto;
import com.bookat.dto.EventResDto;
import com.bookat.dto.EventSeatDto;
import com.bookat.service.impl.EventServiceImpl;

import jakarta.annotation.PostConstruct;

@Component
public class EventScheduler {

	@Autowired
	public EventServiceImpl eventServiceImpl;

	@Autowired
	private StringRedisTemplate redisTemplate;

	// 매일 자정 스케쥴링
	@Scheduled(cron = "0 0 0 * * * ")
	public void preloadEventSeats() {
		// 실제 스케쥴링 코드 : 내일 예매 오픈 예정인 이벤트를 대상으로
		LocalDate targetEventDate = LocalDate.now().plusDays(31); // 이벤트 당일
		preloadSeatsLogic(targetEventDate);
	}

	// 바로 실행용 스케쥴링 (* 프로그램 시작과 동시에 실행되기 때문에 주의)
	// @PostConstruct
	public void preloadEventSeatsByEventId(){
		System.out.println("애플리케이션이 시작되었습니다. 이벤트 좌석을 미리 로드합니다...");
		preloadSeatsLogicByEventId(115);
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

						String totalKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":TOTAL_SEAT";
						String availKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEAT";

						// Redis에 좌석 수 & TTL(만료시간) 저장
						redisTemplate.opsForValue().set(totalKey, String.valueOf(remainingSeat));
						redisTemplate.opsForValue().set(availKey, String.valueOf(remainingSeat));

						redisTemplate.expireAt(totalKey, expireAt);
						redisTemplate.expireAt(availKey, expireAt);

					}
				}

			}

			long endTime = System.currentTimeMillis();
			System.out.println("총 실행 시간 : " + (endTime - startTime) + "ms");

			return ResponseEntity.ok("Preload 완료!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
		}

	}

	// 공통 로직
	public ResponseEntity<String> preloadSeatsLogicByEventId(int eventId) {
		long startTime = System.currentTimeMillis();

		try {
			System.out.println("preloadSeatsLogic By EventId 실행!");

			// 2. events 리스트를 순회하며 eventId로 이벤트 회차 테이블 조회
			// 잔여좌석 값 가져오기
			EventResDto event = eventServiceImpl.selectOne(eventId);
			String ticketType = event.getTicketType();

			// 이벤트 당일
			Date targetDate = event.getEventDate();
			LocalDate localDate = targetDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			
			// Redis TTL 설정을 위한 만료일 : 이벤트 다음날 0시
			LocalDate expireDate = localDate.plusDays(1);
			Date expireAt = Date.from(expireDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

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

					String totalKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":TOTAL_SEAT";
					String availKey = "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEAT";

					// Redis에 좌석 수 & TTL(만료시간) 저장
					redisTemplate.opsForValue().set(totalKey, String.valueOf(remainingSeat));
					redisTemplate.opsForValue().set(availKey, String.valueOf(remainingSeat));

					redisTemplate.expireAt(totalKey, expireAt);
					redisTemplate.expireAt(availKey, expireAt);

				}
			}

			long endTime = System.currentTimeMillis();
			System.out.println("총 실행 시간 : " + (endTime - startTime) + "ms");

			return ResponseEntity.ok("Preload 완료!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생: " + e.getMessage());
		}

	}
}
