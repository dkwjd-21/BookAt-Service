package com.bookat.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.bookat.domain.ReservationStatus;
import com.bookat.enums.PersonType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSessionStore {
	
	// redis 예약 세션 CRUD
	
	private final StringRedisTemplate redisTemplate;
	private static final String KEY_PREFIX = "RESERVATION:";			// 데이터 본문
	private static final String META_PREFIX = "RESERVATION_META:";		// TTL 만료 시 좌석 복구용
	private static final long TTL_SECONDS = 900;						// 만료 15분
	private static final ObjectMapper OM = new ObjectMapper();

	// 예약 세션 생성 (예매 진입 초기값 세팅)
	public String createInit(int eventId, String eventName, String userId, String eventDate) {
		try {
			String token = UUID.randomUUID().toString();
			String key = KEY_PREFIX + token;
			
			Map<String, String> initData = new LinkedHashMap<>();
			initData.put("eventId", String.valueOf(eventId));
			initData.put("eventName", eventName);
			initData.put("userId", userId);
			initData.put("eventDate", eventDate);
			initData.put("status", "STEP1");
			
			redisTemplate.opsForHash().putAll(key, initData);
			redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
			
			return token;
		} catch(Exception e) {
			throw new RuntimeException("예약 세션 생성 실패", e);
		}
	}
	
	// 예약 세션 전체 조회
	public Map<Object, Object> getDataAll(String token) {
		String key = KEY_PREFIX + token;
		
		Map<Object, Object> redisData = redisTemplate.opsForHash().entries(key);
		return (redisData == null || redisData.isEmpty()) ? null : redisData;
	}
	
	// 예약 세션 필드 조회
	public String getDataField(String token, String field) {
		String key = KEY_PREFIX + token;
		
		Object value = redisTemplate.opsForHash().get(key, field);
		return value == null ? null : value.toString();
	}
	
	// step1 update
	public void updateStep1(String token, int scheduleId) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "scheduleId", String.valueOf(scheduleId));
		redisTemplate.opsForHash().put(key, "status", "STEP2");
	}
	
	// 회차를 변경할 경우 관련 값 제거
	public void clearStep2(String token) {
		String key = KEY_PREFIX + token;
		
		// personType
		redisTemplate.opsForHash().delete(key, "reservedCount", "groupCounts", "totalPrice");
	}
	
	// step2 update
	// personType
	public void updateStep2PersonType(String token, int reservedCount, int totalPrice, Map<PersonType, Integer> groupCounts) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "reservedCount", String.valueOf(reservedCount));
		redisTemplate.opsForHash().put(key, "totalPrice", String.valueOf(totalPrice));
		
		Map<String, Integer> groupCountsToString = (groupCounts == null) ? Map.of()
				: groupCounts.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

		try {
			redisTemplate.opsForHash().put(key, "groupCounts", OM.writeValueAsString(groupCountsToString));
		} catch(Exception e) {
			throw new RuntimeException("groupCounts 저장 실패", e);
		}
		
		redisTemplate.opsForHash().put(key, "status", "STEP3");
	}
	
	// seatType
	public void updateStep2SeatType(String token, String seatNamesCsv, int totalPrice) {
		String key = KEY_PREFIX + token;

		redisTemplate.opsForHash().put(key, "seatNames", seatNamesCsv);
		redisTemplate.opsForHash().put(key, "totalPrice", String.valueOf(totalPrice));
		redisTemplate.opsForHash().put(key, "status", "STEP3");
	}
	
	// step3 update
	public void updateStep3(String token, String userName, String phone, String email) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "userName", userName);
		redisTemplate.opsForHash().put(key, "phone", phone);
		redisTemplate.opsForHash().put(key, "email", email);
		redisTemplate.opsForHash().put(key, "status", "STEP4");
	}
	
	// 결제 완료 후 예약 상태
	public void updateReservationStatus(String token, ReservationStatus status) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "reservationStatus", String.valueOf(status.code));
	}
	
	// 임의 step 문자열 갱신
	public void updateStepStatus(String token, String status) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "status", status);
	}
	
	// TTL 만료 시 좌석 복구용 메타데이터 (인원유형)
	public void createPersonMetaDataForSessionExpired(String token, int eventId, int scheduleId, int reservedCount) {
		String metaKey = META_PREFIX + token;
		
		Map<String, String> mataData = new LinkedHashMap<>();
		mataData.put("eventId", String.valueOf(eventId));
		mataData.put("scheduleId", String.valueOf(scheduleId));
		mataData.put("reservedCount", String.valueOf(reservedCount));
		
		redisTemplate.opsForHash().putAll(metaKey, mataData);
	}
	
	// TTL 만료 시 좌석 복구용 메타데이터 (좌석유형)
	public void createSeatMetaDataForSessionExpired(String token, int eventId, int scheduleId, String seatNamesCsv) {
		String metaKey = META_PREFIX + token;
		
		Map<String, String> mataData = new LinkedHashMap<>();
		mataData.put("eventId", String.valueOf(eventId));
		mataData.put("scheduleId", String.valueOf(scheduleId));
		mataData.put("reservedSeats", seatNamesCsv);
		
		redisTemplate.opsForHash().putAll(metaKey, mataData);
	}
	
	// 메타 데이터 조회
	public Map<Object, Object> getMetaData(String token) {
		return redisTemplate.opsForHash().entries(META_PREFIX + token);
	}
	
	// 메타 데이터 삭제
	public void deleteMetaData(String token) {
		redisTemplate.delete(META_PREFIX + token);
	}
	
	// 예약 세션 검증
	public boolean validateReservationSession(String token) {
		
		// 예약 세션 만료 (다시 예약 필요)
		if(!Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token))) {
			return false;
		}
		
		// 예약 세션 유효
		return true;
	}
	
	// 예약 관련 세션 삭제
	public void deleteDataAll(String token) {
		redisTemplate.delete(KEY_PREFIX + token);
		redisTemplate.delete(META_PREFIX + token);
	}
	
	// 인원 유형 json 문자열 파싱
	public Optional<Map<String, Integer>> parseGroupCounts(String json) {
		try {
			if(json == null || json.isBlank()) return Optional.empty();
			Map<String, Integer> data = OM.readValue(json, new TypeReference<Map<String, Integer>>() {});
			return Optional.of(data);
		} catch(Exception e) {
			log.info("groupCounts parse failed: {}", json, e);
			return Optional.empty();
		}
	}
}
