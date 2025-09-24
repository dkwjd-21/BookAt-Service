package com.bookat.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookat.dto.reservation.PaymentInfoResDto;
import com.bookat.dto.EventSeatDto;
import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.SeatTypeReqDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;
import com.bookat.enums.PersonType;
import com.bookat.mapper.EventPartMapper;
import com.bookat.mapper.ReservationMapper;
import com.bookat.service.ReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

	@Autowired
	private final ReservationMapper reservationMapper;
	private final EventPartMapper eventPartMapper;
	private final StringRedisTemplate redisTemplate;
	private final SeatServiceImpl seatService;
	private static final long TTL_SECONDS = 900; // 15분
//	private static final long TTL_SECONDS = 100;	// 테스트 용

	// 예매 시작 (초기 진입)
	@Override
	public ReservationStartDto startReservation(int eventId, String userId) {

		// 이벤트 조회
		Event event = reservationMapper.findEventByEventId(eventId);
		// 이벤트 회차 조회
		List<EventPart> eventParts = eventPartMapper.findEventPartsByEventId(eventId);
		// 이벤트 회차 조회
		eventParts.sort(Comparator.comparing(EventPart::getScheduleTime));

		// 이벤트의 날짜
		Date scheduleTime = eventParts.get(0).getScheduleTime();
		LocalDate eventDate = scheduleTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		// person type - 잔여좌석 불러오기
		if ("PERSON_TYPE".equals(event.getTicketType())) {
			for (EventPart eventPart : eventParts) {
				String redisKey = String.format("EVENT:%d:SCHEDULE:%d:AVAILABLE_SEAT", eventId,
						eventPart.getScheduleId());
				String availableSeat = redisTemplate.opsForValue().get(redisKey);

				if (availableSeat != null) {
					eventPart.setRemainingSeat(Integer.parseInt(availableSeat));
				} else {
					eventPart.setRemainingSeat(0);
				}
			}
		}

		// 예약 토큰 생성
		String reservationToken = UUID.randomUUID().toString();
		String reservationKey = "RESERVATION:" + reservationToken;

		// redis 에 초기 세션 저장 (Hash 구조)
		Map<String, String> reservationData = new HashMap<>();
		reservationData.put("eventId", String.valueOf(eventId));
		reservationData.put("status", "STEP1");
		reservationData.put("userId", userId);
		reservationData.put("eventDate", String.valueOf(eventDate));
		reservationData.put("eventName", event.getEventName());
		redisTemplate.opsForHash().putAll(reservationKey, reservationData);
		redisTemplate.expire(reservationKey, TTL_SECONDS, TimeUnit.SECONDS); // 15분 저장 (변경 가능)

		return new ReservationStartDto(event, eventParts, reservationToken);
	}

	// step1 : 날짜/회차 선택
	@Override
	public void selectSchedule(String reservationToken, int scheduleId) {
		String reservationKey = getReservationTokenKey(reservationToken);

		Map<Object, Object> existingData = redisTemplate.opsForHash().entries(reservationKey);

		String prevEventId = (String) existingData.get("eventId");
		String prevScheduleId = (String) existingData.get("scheduleId");

		// 회차 변경 희망 시 이전 회차와 동일하면 (그냥 변경없이 다시 이후 단계로 진행하고자 할 때) 아무 작업도 하지 않음
		if (prevScheduleId != null && prevScheduleId.equals(String.valueOf(scheduleId))) {
			log.info("같은 회차 재선택: eventId={}, scheduleId={}, 변경 없음", prevEventId, prevScheduleId);
			return;
		}

		// 회차를 변경할 경우 기존 좌석의 복구처리
		if(prevEventId != null && prevScheduleId != null) {
			int prevReserved = Optional.ofNullable(existingData.get("reservedCount"))
					.map(Object::toString)
					.map(Integer::parseInt)
					.orElse(0);
			
			if(prevReserved > 0) {
				String availableSeatsKey = String.format("EVENT:%s:SCHEDULE:%s:AVAILABLE_SEAT", prevEventId, prevScheduleId);

				redisTemplate.opsForValue().increment(availableSeatsKey, prevReserved);
				log.info("회차 변경으로 좌석 복구: eventId={}, scheduleId={}, 복구좌석={}", prevEventId, prevScheduleId,
						prevReserved);
			}
			
			redisTemplate.opsForHash().delete(reservationKey, "reservedCount", "groupCounts", "totalPrice");
			
			// 기존 메타 삭제
		    redisTemplate.delete("RESERVATION_META:" + reservationToken);

			for (PersonType type : PersonType.values()) {
				redisTemplate.opsForHash().delete(reservationKey, type.name() + "_COUNT");
			}
			redisTemplate.opsForHash().delete(reservationKey, "totalPrice");

			// 기존 메타 삭제
			redisTemplate.delete("RESERVATION_META:" + reservationToken);
		}

		// redis 에 정보 반영 (변경할경우는 덮어쓰기)
		redisTemplate.opsForHash().put(reservationKey, "scheduleId", String.valueOf(scheduleId));
		redisTemplate.opsForHash().put(reservationKey, "status", "STEP2");
	}

	// step2 : 인원등급/인원수 선택
	@Override
	public void selectPersonType(String reservationToken, PersonTypeReqDto personTypeReqDto) {
		String reservationKey = getReservationTokenKey(reservationToken);

		String eventId = (String) redisTemplate.opsForHash().get(reservationKey, "eventId");
		String scheduleId = (String) redisTemplate.opsForHash().get(reservationKey, "scheduleId");

		if (eventId == null || scheduleId == null) {
			throw new IllegalStateException("예약 토큰에 회차 정보가 없습니다.");
		}

		String availableSeatsKey = String.format("EVENT:%s:SCHEDULE:%s:AVAILABLE_SEAT", eventId, scheduleId);

		// 현재 잔여 좌석 확인
		int availableSeats = Optional.ofNullable(redisTemplate.opsForValue().get(availableSeatsKey))
				.map(Integer::parseInt)
				.orElse(0);
		
		// 기존에 요청받았던 인원
		int prevTotal = Optional.ofNullable(redisTemplate.opsForHash().get(reservationKey, "reservedCount"))
				.map(Object::toString)
				.map(Integer::parseInt)
				.orElse(0);

		// 새로 요청받은 인원 합계
		int totalPersonCount = personTypeReqDto.getPersonCounts().values().stream().mapToInt(Integer::intValue).sum();

		int diffPersonCount = totalPersonCount - prevTotal;

		if (diffPersonCount > 0) {
			// 인원 초과 에러
			if (diffPersonCount > availableSeats) {
				throw new IllegalArgumentException(
						String.format("잔여 좌석 부족: 요청 %d석, 잔여 %d석", totalPersonCount, availableSeats));
			}
			// 인원 증가 -> 잔여 좌석 차감
			redisTemplate.opsForValue().decrement(availableSeatsKey, diffPersonCount);
		} else if (diffPersonCount < 0) {
			// 인원 감소 -> 잔여 좌석 복구
			redisTemplate.opsForValue().increment(availableSeatsKey, Math.abs(diffPersonCount));
		}

		// 인원등급별 인원수와 총 금액 저장
		Map<String, String> redisData = new HashMap<>();

		redisData.put("reservedCount", String.valueOf(totalPersonCount));
		redisData.put("totalPrice", String.valueOf(personTypeReqDto.getTotalPrice()));
		redisData.put("status", "STEP3");
		
		try {
			String groupCountsJson = new ObjectMapper().writeValueAsString(personTypeReqDto.getPersonCounts());
			redisData.put("groupCounts", groupCountsJson);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("groupCounts 직렬화 실패", e);
		}
		
		redisTemplate.opsForHash().putAll(reservationKey, redisData);

		// TTL 만료 시 잔여 좌석 복구를 위해 필요한 메타데이터도 생성 (TTL 만료되면 예약해둔 좌석정보도 사라지기 때문)
		// TTL 없는 별도 키에 복구용 최소 데이터 저장
		String metaKey = "RESERVATION_META:" + reservationToken;

		Map<String, String> metaData = new HashMap<>();
		metaData.put("eventId", eventId);
		metaData.put("scheduleId", scheduleId);
		metaData.put("reservedCount", String.valueOf(totalPersonCount));

		redisTemplate.opsForHash().putAll(metaKey, metaData);

	}

	// step2 : 좌석 선택
	@Override
	public void selectSeatType(String reservationToken, SeatTypeReqDto reqDto) {
		// 좌석 유효성 검증
		boolean allAvailable = seatService.checkAllSeatsAvailable(reqDto.getEventId(), reqDto.getScheduleId(),
				reqDto.getSeatNames());

		if (!allAvailable) {
			throw new IllegalArgumentException("다른 고객님께서 이미 선택한 좌석입니다.");
		}

		// 좌석 홀드
		boolean success = seatService.holdSeats(reqDto.getEventId(), reqDto.getScheduleId(), reqDto.getSeatNames());
		if (!success) {
			throw new IllegalArgumentException("다른 고객님께서 이미 선택한 좌석입니다.");
		}

		// 예약 토큰에 좌석 정보 및 총 금액 저장
		String reservationKey = getReservationTokenKey(reservationToken);
		Map<String, String> redisData = new HashMap<>();
		redisData.put("seatNames", String.join(",", reqDto.getSeatNames()));
		redisData.put("totalPrice", String.valueOf(reqDto.getTotalPrice()));
		redisData.put("status", "STEP3");
		redisTemplate.opsForHash().putAll(reservationKey, redisData);

		// TTL 만료 대비 메타 데이터 생성
		String metaKey = "RESERVATION_META:" + reservationToken;
		Map<String, String> metaData = new HashMap<>();
		metaData.put("eventId", String.valueOf(reqDto.getEventId()));
		metaData.put("scheduleId", String.valueOf(reqDto.getScheduleId()));
		metaData.put("reservedSeats", String.join(",", reqDto.getSeatNames()));
		redisTemplate.opsForHash().putAll(metaKey, metaData);
	}

	// step3 : 주문자 정보 입력
	@Override
	public boolean inputUserInfo(String reservationToken, String userId, UserInfoReqDto userInfoReqDto) {
		String reservationKey = getReservationTokenKey(reservationToken);

		String redisUserId = (String) redisTemplate.opsForHash().get(reservationKey, "userId");
		if (redisUserId == null || !redisUserId.equals(userId)) {
			log.info("유저 불일치");
			return false;
		}

		redisTemplate.opsForHash().put(reservationKey, "userName", userInfoReqDto.getUserName());
		redisTemplate.opsForHash().put(reservationKey, "phone", userInfoReqDto.getPhone());
		redisTemplate.opsForHash().put(reservationKey, "email", userInfoReqDto.getEmail());

		redisTemplate.opsForHash().put(reservationKey, "status", "STEP4");

		return true;

	}

	// 좌석 확정 로직
	@Override
	public void confirmBooking(String reservationToken, SeatTypeReqDto reqDto) {
		// 좌석 DB 상태 변경
		for (String seatName : reqDto.getSeatNames()) {
			EventSeatDto seatDto = new EventSeatDto();
			seatDto.setEventId(reqDto.getEventId());
			seatDto.setScheduleId(reqDto.getScheduleId());
			seatDto.setSeatName(seatName);
			seatDto.setSeatStatus(0); // 0 = 예약 완료
			seatService.updateSeatStatus(seatDto);
		}

		// Redis 예약 정보 업데이트
		String reservationKey = getReservationTokenKey(reservationToken);
		Map<String, String> redisData = new HashMap<>();
		redisData.put("status", "COMPLETED");
		// 이미 값이 존재할 경우 덮어쓰기가 됨
		redisData.put("seatNames", String.join(",", reqDto.getSeatNames()));
		redisData.put("totalPrice", String.valueOf(reqDto.getTotalPrice()));
		redisTemplate.opsForHash().putAll(reservationKey, redisData);

		// Redis 좌석 상태 업데이트
		String seatsHashKey = String.format("EVENT:%d:SCHEDULE:%d:SEATS", reqDto.getEventId(), reqDto.getScheduleId());
		String holdSetKey = String.format("EVENT:%d:SCHEDULE:%d:HOLDED_SEATS", reqDto.getEventId(),
				reqDto.getScheduleId());
		String bookedSetKey = String.format("EVENT:%d:SCHEDULE:%d:BOOKED_SEATS", reqDto.getEventId(),
				reqDto.getScheduleId());

		for (String seatName : reqDto.getSeatNames()) {
		    // Hash에서 상태 변경
		    redisTemplate.opsForHash().put(seatsHashKey, seatName, "0"); // 0 = 예약 완료

		    // Set 이동: 홀드 -> booked
		    redisTemplate.opsForSet().remove(holdSetKey, seatName);
		    redisTemplate.opsForSet().add(bookedSetKey, seatName);
		}
		
		// TTL 기반 복구용 메타 삭제
	    redisTemplate.delete("RESERVATION_META:" + reservationToken);
	}

	// 좌석 취소 로직
	@Override
	public void cancelReservation(String reservationToken) {
		String reservationKey = getReservationTokenKey(reservationToken);

		if (!Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey))) {
			log.warn("취소 시도: 유효하지 않은 예약 토큰 [{}]", reservationToken);
			return;
		}

		Map<Object, Object> data = redisTemplate.opsForHash().entries(reservationKey);
		if (data.isEmpty())
			return;

		String eventId = (String) data.get("eventId");
		String scheduleId = (String) data.get("scheduleId");
		String seatNamesStr = (String) data.get("seatNames");
		
		if (eventId == null || scheduleId == null) {
	        log.warn("취소 실패: eventId 또는 scheduleId 없음 [{}]", reservationToken);
	        redisTemplate.delete(reservationKey);
	        redisTemplate.delete("RESERVATION_META:" + reservationToken);
	        return;
	    }
		
		// 좌석 이름이 존재 -> SEAT_TYPE 처리 
		if(seatNamesStr != null && !seatNamesStr.isEmpty()) {
			List<String> seatNames = Arrays.stream(seatNamesStr.split(","))
						 				   .map(String::trim)
						 				   .filter(s -> !s.isEmpty())
						 				   .collect(Collectors.toList());
			if(!seatNames.isEmpty()) {
				try {
					int evtId = Integer.parseInt(eventId);
					int schId = Integer.parseInt(scheduleId);
					seatService.releaseSeats(evtId, schId, seatNames);
					log.info("예약 취소 시 좌석 초기화 완료");
				} catch (Exception e) {
					log.error("좌석 초기화 중 오류 발생", e);
				}
			}
			
		} else {
			// 좌석 정보가 없으면 PERSON_TYPE 처리 
			String availableSeatsKey = String.format("EVENT:%s:SCHEDULE:%s:AVAILABLE_SEAT", eventId, scheduleId);

			// 예약된 전체 인원의 수 구하기
			int totalReserved = 0;

			Object reservedCount = data.get("reservedCount");
			
			if(reservedCount != null) {
				totalReserved = Integer.parseInt(reservedCount.toString());

			}

			// 취소시에는 예약된 인원 수만큼 잔여 좌석 복구
			if (totalReserved > 0) {
				redisTemplate.opsForValue().increment(availableSeatsKey, totalReserved);
				log.info("좌석 복구 완료: eventId={}, scheduleId={}, 복구좌석={}", eventId, scheduleId, totalReserved);
			}

		}
		// 나중에 결제 세션도 함께 삭제 구현하기
//		String paymentKey = "payment:" + "";

		// 예약 데이터와 meta 키 삭제 
		redisTemplate.delete(reservationKey);
		redisTemplate.delete("RESERVATION_META:" + reservationToken);

		log.info("예약 취소 완료: reservationToken={}", reservationKey);
	}

	@Override
	public void validateReservation(String reservationToken) {
		String reservationKey = "RESERVATION:" + reservationToken;

		if (!Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey))) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}
	}
	
	@Override
	public PaymentInfoResDto getPaymentInfo(String reservationToken) {
		String reservationKey = getReservationTokenKey(reservationToken);
		
		Map<Object, Object> redisData = redisTemplate.opsForHash().entries(reservationKey);
		
		int eventId = Integer.parseInt(redisData.get("eventId").toString());
		int scheduleId = Integer.parseInt(redisData.get("scheduleId").toString());
		String title = (String) redisData.get("title");
		int totalPrice = Integer.parseInt(redisData.get("totalPrice").toString());
		int reservedCount = Integer.parseInt(redisData.get("reservedCount").toString());
		
		PaymentInfoResDto paymentInfoResDto = new PaymentInfoResDto();
		paymentInfoResDto.setEventId(eventId);
		paymentInfoResDto.setScheduleId(scheduleId);
		paymentInfoResDto.setTitle(title);
		paymentInfoResDto.setTotalPrice(totalPrice);
		paymentInfoResDto.setReservedCount(reservedCount);
		
		return paymentInfoResDto;
	}

	// =========================================================================================================

	// 결제 완료 후 reservation 1건 + ticket N건을 생성
	@Transactional
	@Override
	public int createReservationAndTicket(String paymentToken, String reservationToken) {
		
		String reservationKey = getReservationTokenKey(reservationToken);
		Map<Object, Object> reservationData = redisTemplate.opsForHash().entries(reservationKey);
		if(reservationData == null || reservationData.isEmpty()) {
			throw new IllegalStateException("예약 세션이 만료되었거나 존재하지 않습니다.");
		}
		
		// 예약 생성
		Reservation reservation = new Reservation();
		// 결제 시 생성된 결제테이블의 결제아이디
		// 결제테이블에 payment info 에 이벤트 이름이 들어가야함!
		reservation.setPaymentId(1);
		return 0;
	}

	private void insertPersonTicket(int reservationId, int paymentId, PersonType personType, int personCount) {
		for (int i = 0; i < personCount; i++) {
			Ticket ticket = new Ticket();

			ticket.setTicketCreatedDate(new Date());
			ticket.setTicketStatus(1);
			ticket.setTicketType("PERSON_TYPE");
			ticket.setPersonType(personType);
			ticket.setReservationId(reservationId);
			ticket.setSeatId(null);
			ticket.setPaymentId(paymentId);

			reservationMapper.insertTicket(ticket);
		}
	}
	
	private String getReservationTokenKey(String reservationToken) {
		String reservationKey = "RESERVATION:" + reservationToken;
		
		if(!Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey))) {
			throw new IllegalStateException("예약 세션이 만료되었습니다. 다시 예약해주세요.");
		}
		
		return reservationKey;
	}

	@Override
	public List<EventPart> selectPartsByEventId(int eventId) {
		return reservationMapper.findPartsByEventId(eventId);
	}

}
