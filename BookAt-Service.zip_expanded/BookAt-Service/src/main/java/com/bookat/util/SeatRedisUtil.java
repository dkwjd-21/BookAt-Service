package com.bookat.util;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SeatRedisUtil {

	@Autowired
	private StringRedisTemplate redisTemplate;

	// 좌석 상태(Hash) 조회
	// [Key] EVENT:{eventId}:SCHEDULE:{scheduleId}:SEATS
	// [Value] { seatName : status }
	public Map<Object, Object> getSeatMap(int eventId, int scheduleId) {
		String hashKey = buildSeatsKey(eventId, scheduleId);
		return redisTemplate.opsForHash().entries(hashKey);
	}

	// 특정 좌석이 AVAILABLE SET에 포함되어 있는지 확인
	// [Key] EVENT:{eventId}:SCHEDULE:{scheduleId}:AVAILABLE_SEATS
	public boolean isSeatAvailable(int eventId, int scheduleId, String seatName) {
		String availableSetKey = buildAvailableSeatsKey(eventId, scheduleId);
		Boolean result = redisTemplate.opsForSet().isMember(availableSetKey, seatName);
		return (result != null) && result;
	}

	// 좌석 HOLD 처리
	// [SMOVE] AVAILABLE -> HOLD 셋으로 이동
	// [Hash] 좌석 상태를 "HOLD"로 변경
	public List<Object> holdSeats(int eventId, int scheduleId, List<String> seatNames) {
		String hashKey = buildSeatsKey(eventId, scheduleId);
		String availableSetKey = buildAvailableSeatsKey(eventId, scheduleId);
		String holdSetKey = buildHoldSeatsKey(eventId, scheduleId);

		// 파이프라인으로 한번에 처리
		return redisTemplate.executePipelined((RedisCallback<Void>) connection -> {
			for (String seatName : seatNames) {
				// AVAILABLE -> HOLD 이동
				connection.setCommands().sMove(availableSetKey.getBytes(), holdSetKey.getBytes(), seatName.getBytes());

				// Hash : HOLD 업데이트
				connection.hashCommands().hSet(hashKey.getBytes(), seatName.getBytes(), "HOLD".getBytes());
			}
			// 여기서 return null은 명령어를 모두 작성했음을 의미함
			return null;
		});
	}

	// 좌석 해제 처리
	// [Set] HOLD/BOOKED 셋에서 제거 -> AVAILABLE 셋에 추가
	// [Hash] AVAILABLE로 업데이트
	public void releaseSeat(int eventId, int scheduleId, String seatName) {
		String hashKey = buildSeatsKey(eventId, scheduleId);
		String availableSetKey = buildAvailableSeatsKey(eventId, scheduleId);
		String holdSetKey = buildHoldSeatsKey(eventId, scheduleId);
		String bookedSetKey = buildBookedSeatsKey(eventId, scheduleId);

		redisTemplate.opsForSet().remove(holdSetKey, seatName);
		redisTemplate.opsForSet().remove(bookedSetKey, seatName);
		redisTemplate.opsForSet().add(availableSetKey, seatName);
		redisTemplate.opsForHash().put(hashKey, seatName, "AVAILABLE");
	}

	/* ---------------------- Key Builder ---------------------- */
	// 좌석 상태 (Hash)
	private String buildSeatsKey(int eventId, int scheduleId) {
		return "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":SEATS";
	}

	// 예약 가능 좌석 (Set)
	private String buildAvailableSeatsKey(int eventId, int scheduleId) {
		return "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":AVAILABLE_SEATS";
	}

	// HOLD 좌석 (Set)
	private String buildHoldSeatsKey(int eventId, int scheduleId) {
		return "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":HOLD_SEATS";
	}

	// BOOKED 좌석 (Set)
	private String buildBookedSeatsKey(int eventId, int scheduleId) {
		return "EVENT:" + eventId + ":SCHEDULE:" + scheduleId + ":BOOKED_SEATS";
	}

}
