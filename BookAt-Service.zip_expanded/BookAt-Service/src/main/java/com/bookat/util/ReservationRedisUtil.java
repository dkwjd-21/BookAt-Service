package com.bookat.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.bookat.domain.PaymentStatus;
import com.bookat.domain.ReservationStatus;
import com.bookat.enums.PersonType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationRedisUtil {

	// redis 예약 세션 CRUD
	private final StringRedisTemplate redisTemplate;
	private final QueueUtil queueUtil;
	private final PaymentSessionStore paymentSessionStore;
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
	
	// 회차 변경 시 이전 회차 좌석 복구 + Step2 필드/메타 삭제 원자적 처리
	public int rollbackOnScheduleChange(String token, String prevEventId, String prevScheduleId) {
		String key = KEY_PREFIX + token;
		String metaKey = META_PREFIX + token;
		String availableSeatsKey = getAvailableSeatsKey(prevEventId, prevScheduleId);
		
	    String luaScript =
	    			"local reservedCount = redis.call('HGET', KEYS[1], 'reservedCount') " +
	    			"if reservedCount then " +
	    			"   local count = tonumber(reservedCount) " +
	    			"   if count > 0 then " +
	    			"       redis.call('INCRBY', KEYS[2], count) " +									// [잔여좌석] 좌석 복구
	    			"   end " +
	    			"   redis.call('HDEL', KEYS[1], 'reservedCount', 'personCounts', 'totalPrice') " +	// [예약세션] STEP2 관련 필드 삭제
	    			"end " +
	    			"redis.call('DEL', KEYS[3]) " +														// 메타 삭제
	    			"return reservedCount or 0";
		
	    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
	    redisScript.setScriptText(luaScript);
	    redisScript.setResultType(Long.class);
	    
	    Long result = redisTemplate.execute(redisScript, Arrays.asList(key, availableSeatsKey, metaKey));
		
	    return result != null ? result.intValue() : 0;
	}
	
	// STEP2 personType 세션 정보 업데이트 및 좌석차감/복구 원자적 처리
	// 하나의 LuaScript 에서 좌석 증감 + 세션 hash 업데이트 처리
	public int adjustSeatsAndUpdateStep2PT(String token, String eventId, String scheduleId, int diff, int reservedCount, int totalPrice, Map<PersonType, Integer> personCounts) {
		try {
			String key = KEY_PREFIX + token;
			String availableSeatsKey = getAvailableSeatsKey(eventId, scheduleId);
			
			Map<String, Integer> personCountsToStr = (personCounts == null) ? Map.of()
					: personCounts.entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
			
			String personCountsJson = OM.writeValueAsString(personCountsToStr);
			
			String luaScript = 
						"local available = tonumber(redis.call('GET', KEYS[1]) or '0') " +
						"local diff = tonumber(ARGV[1]) " +
						"local reservedCount = ARGV[2] " +
						"local totalPrice = ARGV[3] " +
						"local personCounts = ARGV[4] " +
						"if diff > 0 then " +
						"  if available < diff then " +
						"    return -1 " +													// [잔여좌석] 잔여좌석 수보다 많으면 -1 반환
						"  end " +
						"  redis.call('DECRBY', KEYS[1], diff) " +							// [잔여좌석] 좌석 차감
						"elseif diff < 0 then " +
						"  redis.call('INCRBY', KEYS[1], -diff) " +							// [잔여좌석] 좌석 복구
						"end " +
						"redis.call('HSET', KEYS[2], 'reservedCount', reservedCount) " +	// [예약세션] 총 선택인원 추가
						"redis.call('HSET', KEYS[2], 'totalPrice', totalPrice) " +			// [예약세션] 총 금액 추가
						"redis.call('HSET', KEYS[2], 'personCounts', personCounts) " +		// [예약세션] 인원 등급 별 인원 수 추가
						"redis.call('HSET', KEYS[2], 'status', 'STEP3') " +					// [예약세션] 단계 상태 업데이트
						"return tonumber(redis.call('GET', KEYS[1]))";
			
			DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
			redisScript.setScriptText(luaScript);
			redisScript.setResultType(Long.class);
			
			Long result = redisTemplate.execute(
					redisScript, 
					List.of(availableSeatsKey, key), 
					String.valueOf(diff),
					String.valueOf(reservedCount),
					String.valueOf(totalPrice),
					personCountsJson);
			
			return result != null ? result.intValue() : -1;
		} catch(Exception e) {
			throw new RuntimeException("STEP2 업데이트 실패", e);
		}
	}
	
	// 잔여좌석 조회
	public int getAvailableSeats(String eventId, String scheduleId) {
		String availableSeatsKey = getAvailableSeatsKey(eventId, scheduleId);
		String value = redisTemplate.opsForValue().get(availableSeatsKey);
		return value != null ? Integer.parseInt(value) : 0;
	}
	
	// 잔여좌석 다건 조회
	public Map<String, Integer> getAvailableSeatsMap(String eventId, List<String> scheduleList) {
		List<String> rediskeys = scheduleList.stream()
				.map(scheduleId -> getAvailableSeatsKey(eventId, scheduleId))
				.toList();
		
		List<String> seatValues = redisTemplate.opsForValue().multiGet(rediskeys);
		
		Map<String, Integer> availableSeatsMap = new HashMap<>();
		for(int i = 0; i < scheduleList.size(); i++) {
			String seatStr = seatValues.get(i);
			int remainingSeats = (seatStr != null) ? Integer.parseInt(seatStr) : 0;
			availableSeatsMap.put(scheduleList.get(i), remainingSeats);
		}
		
		return availableSeatsMap;
	}

	// seatType
	public void updateStep2SeatType(String token, String seatNamesCsv, int totalPrice, int reservedCount) {
		String key = KEY_PREFIX + token;

		redisTemplate.opsForHash().put(key, "seatNames", seatNamesCsv);
		redisTemplate.opsForHash().put(key, "totalPrice", String.valueOf(totalPrice));
		redisTemplate.opsForHash().put(key, "reservedCount", String.valueOf(reservedCount));
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
	
	// 결제창 진입시 생성되는 결제세션토큰값 업데이트
	public boolean updatePaymentSessionToken(String reservationToken, String paymentToken) {
		String key = KEY_PREFIX + reservationToken;
		
		Object existing = redisTemplate.opsForHash().get(key, "paymentToken");
		
		if(existing != null) {
			if(paymentToken.equals(existing.toString())) {
				// 이미 같은 값이면 그대로 두기 (멱등 처리)
				log.debug("Reservation [{}] already mapped to paymentToken [{}], skip update.", reservationToken, paymentToken);
				return true;
			} else {
				// 다른 paymentToken이 이미 매핑되어 있음, 정책적으로 덮어쓰지 않음
				log.warn("Reservation [{}] already mapped to different paymentToken [{}] (new [{}]). Keeping existing.", 
                        reservationToken, existing, paymentToken);
				return false;
			}
		}
		
		redisTemplate.opsForHash().put(key, "paymentToken", paymentToken);
		
		return true;
	}
	
	// 결제 완료 후 예약 상태
	public void updateReservationStatus(String token, ReservationStatus reservationStatus) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "reservationStatus", reservationStatus.name());
		redisTemplate.opsForHash().delete(key, "paymentToken");
	}
	
	// 임의 step 문자열 갱신
	public void updateStepStatus(String token, String status) {
		String key = KEY_PREFIX + token;
		
		redisTemplate.opsForHash().put(key, "status", status);
	}
	
	// TTL 만료 시 좌석 복구용 메타데이터 (인원유형)
	public void createMetaDataForSessionExpired(String token, int eventId, int scheduleId, int reservedCount, String userId) {
		String metaKey = META_PREFIX + token;
		
		Map<String, String> mataData = new LinkedHashMap<>();
		mataData.put("eventId", String.valueOf(eventId));
		mataData.put("scheduleId", String.valueOf(scheduleId));
		mataData.put("reservedCount", String.valueOf(reservedCount));
		mataData.put("userId", userId);
		
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
			int result = rollbackOnCancel(token);
			log.info("{} 예약 세션 만료 좌석 복구 완료", result);
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
	
	// 결제 진행 전 결제 세션 삭제 (STEP4에서 브라우저 종료 or 뒤로가기)
	public void deletePaymentData(String token) {
		String key = KEY_PREFIX + token;
		
		Object paymentTokenObj = redisTemplate.opsForHash().get(key, "paymentToken");
		String paymentToken = (paymentTokenObj != null) ? paymentTokenObj.toString() : null;
		
		if(paymentToken != null) {
			String paymentTokenKey = "payment:" + paymentToken;
			Object paymentStatusObj = redisTemplate.opsForHash().get(paymentTokenKey, "status");
			String paymentStatus = (paymentStatusObj.toString() != null) ? paymentStatusObj.toString() : null;
			
			if(!PaymentStatus.PAID.name().equals(paymentStatus)) {
				paymentSessionStore.consumeEventPay(paymentToken);
				redisTemplate.opsForHash().delete(key, "paymentToken");
				log.info("STEP4, 부여됐던 결제세션은 결제창 이탈로 인한 삭제");
			}
		}
	}
	
	// 예약 프로세스 취소 시 좌석 복구와 관련 세션 삭제의 원자적 처리
	// 예약 취소 시 좌석 복구 + 세션/메타 삭제
	public int rollbackOnCancel(String token) {
		String key = KEY_PREFIX + token;
		String metaKey = META_PREFIX + token;
		
		Map<Object, Object> metaData = redisTemplate.opsForHash().entries(metaKey);
		
		if (metaData == null || metaData.isEmpty()) {
			log.warn("META 데이터 없음, 좌석 복구 없이 세션만 삭제");
			// Active Set 에서 사용자 제거
//			queueUtil.leaveActive(eventId, userId);
			redisTemplate.delete(key);
			return 0;
		}
		
		String eventId = metaData.get("eventId").toString();
		String scheduleId = metaData.get("scheduleId").toString();
		String userId = metaData.get("userId").toString();
		String availableSeatsKey = getAvailableSeatsKey(eventId, scheduleId);
		
		// Active Set 에서 사용자 제거
		queueUtil.leaveActive(eventId, userId);
		
		String luaScript = 
					"local reservedCount = redis.call('HGET', KEYS[3], ARGV[1]) " +
					"if reservedCount then " +
					"   local count = tonumber(reservedCount) " +
					"   if count > 0 then " +
					"       redis.call('INCRBY', KEYS[2], count) " +	// 잔여 좌석 복구
					"   end " +
					"end " +
					"redis.call('DEL', KEYS[1]) " +     				// 예약세션 삭제
					"redis.call('DEL', KEYS[3]) " +     				// 메타 키 삭제
					"return reservedCount or 0";
		
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(luaScript);
		redisScript.setResultType(Long.class);
		
		Long result = redisTemplate.execute(redisScript, Arrays.asList(key, availableSeatsKey, metaKey), "reservedCount");
		
		return result != null ? result.intValue() : 0;
	}
	
	// 인원 유형 json 문자열 파싱
	public Optional<Map<String, Integer>> parsePersonCounts(String json) {
		try {
			if(json == null || json.isBlank()) return Optional.empty();
			Map<String, Integer> data = OM.readValue(json, new TypeReference<Map<String, Integer>>() {});
			return Optional.of(data);
		} catch(Exception e) {
			log.info("personCounts parse failed: {}", json, e);
			return Optional.empty();
		}
	}
	
	// 잔여좌석 키
	private String getAvailableSeatsKey(String eventId, String scheduleId) {
		return String.format("EVENT:%s:SCHEDULE:%s:AVAILABLE_SEAT", eventId, scheduleId);
	}
}
