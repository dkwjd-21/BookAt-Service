package com.bookat.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.enums.PersonType;
import com.bookat.mapper.EventPartMapper;
import com.bookat.mapper.ReservationMapper;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
	
	private final ReservationMapper reservationMapper;
	private final EventPartMapper eventPartMapper;
	private final StringRedisTemplate redisTemplate;
	private static final long TTL_SECONDS = 900;	// 15분

	// 예매 시작 (초기 진입)
	@Override
	public ReservationStartDto startReservation(int eventId, String userId) {
		
		// 이벤트 조회
		Event event = reservationMapper.findEventByEventId(eventId);
		// 이벤트 회차 조회
		List<EventPart> eventParts = eventPartMapper.findEventPartsByEventId(eventId);
		
		// 이벤트의 날짜
		Date scheduleTime = eventParts.get(0).getScheduleTime();
		LocalDate eventDate = scheduleTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		
		for(EventPart eventPart : eventParts) {
			String redisKey = String.format("EVENT:%d:SCHEDULE:%d:AVAILABLE_SEAT", eventId, eventPart.getScheduleId());
			String availableSeat = redisTemplate.opsForValue().get(redisKey);
			
			if(availableSeat != null) {
				eventPart.setRemainingSeat(Integer.parseInt(availableSeat));
			} else {
				eventPart.setRemainingSeat(0);
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
		
		redisTemplate.opsForHash().putAll(reservationKey, reservationData);
		redisTemplate.expire(reservationKey, TTL_SECONDS, TimeUnit.SECONDS);	// 15분 저장 (변경 가능)
		
		return new ReservationStartDto(event, eventParts, reservationToken);
	}
	
	// step1 : 날짜/회차 선택
	@Override
	public void selectSchedule(String reservationToken, int scheduleId) {
		String reservationKey = getReservationTokenKey(reservationToken);
		
		redisTemplate.opsForHash().put(reservationKey, "scheduleId", String.valueOf(scheduleId));
		redisTemplate.opsForHash().put(reservationKey, "status", "STEP2");
	}
	
	// step1 : 인원등급/인원수 선택
	@Override
	public void selectPersonType(String reservationToken, PersonTypeReqDto personTypeReqDto) {
		String reservationKey = getReservationTokenKey(reservationToken);
		
		String eventId = (String) redisTemplate.opsForHash().get(reservationKey, "eventId");
		String scheduleId = (String) redisTemplate.opsForHash().get(reservationKey, "scheduleId");
		
		if(eventId == null || scheduleId == null) {
			throw new IllegalStateException("예약 토큰에 회차 정보가 없습니다.");
		}
		
		String availableSeatsKey = String.format("EVENT:%s:SCHEDULE:%s:AVAILABLE_SEAT", eventId, scheduleId);
		
		// 현재 잔여 좌석 확인
		String availableSeatsStr = redisTemplate.opsForValue().get(availableSeatsKey);
		int availableSeats = availableSeatsStr == null ? 0 : Integer.parseInt(availableSeatsStr);
		
		// 기존에 요청받았던 인원
		int prevTotal = 0;
		Map<Object, Object> existingData = redisTemplate.opsForHash().entries(reservationKey);
		for(PersonType type : PersonType.values()) {
			Object val = existingData.get(type.name() + "_COUNT");
			if(val != null) {
				prevTotal += Integer.parseInt(val.toString());
			}
		}
		
		// 새로 요청받은 인원 합계
		int totalPersonCount = personTypeReqDto.getPersonCounts()
				.values()
				.stream()
				.mapToInt(Integer::intValue)
				.sum();
		
		int diffPersonCount = totalPersonCount - prevTotal;
		
		if(diffPersonCount > 0) {
			// 인원 초과 에러
			if(diffPersonCount > availableSeats) {
				throw new IllegalArgumentException(String.format("잔여 좌석 부족: 요청 %d석, 잔여 %d석", totalPersonCount, availableSeats));
			}
			// 인원 증가 -> 잔여 좌석 차감
			redisTemplate.opsForValue().decrement(availableSeatsKey, diffPersonCount);
		} else if(diffPersonCount < 0) {
			// 인원 감소 -> 잔여 좌석 복구
			redisTemplate.opsForValue().increment(availableSeatsKey, Math.abs(diffPersonCount));
		}
		
		// 인원등급별 인원수와 총 금액 저장
		Map<String, String> redisData = new HashMap<>();
		personTypeReqDto.getPersonCounts().forEach((type, count) -> {
			String personTypeKey = String.format("%s_COUNT",  type);
			redisData.put(personTypeKey, String.valueOf(count));
		});
		
		redisData.put("totalPrice", String.valueOf(personTypeReqDto.getTotalPrice()));
		redisData.put("status", "STEP3");
		
		redisTemplate.opsForHash().putAll(reservationKey, redisData);
		
	}
	
	// step3 : 주문자 정보 입력
	@Override
	public boolean inputUserInfo(String reservationToken, String userId, UserInfoReqDto userInfoReqDto) {
		String reservationKey = getReservationTokenKey(reservationToken);
		
		String redisUserId = (String) redisTemplate.opsForHash().get(reservationKey, "userId");
		if(redisUserId == null || !redisUserId.equals(userId)) {
			log.info("유저 불일치");
			return false;
		}
		
		redisTemplate.opsForHash().put(reservationKey, "userName", userInfoReqDto.getUserName());
		redisTemplate.opsForHash().put(reservationKey, "phone", userInfoReqDto.getPhone());
		redisTemplate.opsForHash().put(reservationKey, "email", userInfoReqDto.getEmail());
		
		redisTemplate.opsForHash().put(reservationKey, "status", "STEP4");
		
		return true;
		
	}
	
	private String getReservationTokenKey(String reservationToken) {
		String reservationKey = "RESERVATION:" + reservationToken;
		
		if(!Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey))) {
			throw new RuntimeException("유효하지 않은 예약 토큰입니다.");
		}
		
		return reservationKey;
	}

}
