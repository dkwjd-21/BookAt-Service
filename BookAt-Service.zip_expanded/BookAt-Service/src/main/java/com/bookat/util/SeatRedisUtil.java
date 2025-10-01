package com.bookat.util;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
	public Long holdSeats(int eventId, int scheduleId, List<String> seatNames) {
		String hashKey = buildSeatsKey(eventId, scheduleId);
		String availableSetKey = buildAvailableSeatsKey(eventId, scheduleId);
		String holdSetKey = buildHoldSeatsKey(eventId, scheduleId);

		// 루아스크립트로 원자적 처리
		String luaScript = "local successCount = 0 " + "for i, seat in ipairs(ARGV) do "
				+ "  if redis.call('SISMEMBER', KEYS[1], seat) == 1 then " + // AVAILABLE에 있을 때만
				"    redis.call('SMOVE', KEYS[1], KEYS[2], seat) " + // AVAILABLE -> HOLD
				"    redis.call('HSET', KEYS[3], seat, 'HOLD') " + // Hash 상태 변경
				"    successCount = successCount + 1 " + "  end " + "end " + "return successCount";

		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(luaScript);
		redisScript.setResultType(Long.class);

		return redisTemplate.execute(redisScript, List.of(availableSetKey, holdSetKey, hashKey),
				seatNames.toArray(new String[0]));
	}

	// 좌석 확정 처리 (BOOKED)
	// [Set] HOLD -> BOOKED
	// [Hash] BOOKED로 상태 업데이트
	public Long bookSeats(int eventId, int scheduleId, List<String> seatNames) {
		String hashKey = buildSeatsKey(eventId, scheduleId);
		String holdSetKey = buildHoldSeatsKey(eventId, scheduleId);
		String bookedSetKey = buildBookedSeatsKey(eventId, scheduleId);

		String luaScript = "local count = 0 " + "for i, seat in ipairs(ARGV) do "
				+ "  if redis.call('SISMEMBER', KEYS[1], seat) == 1 then " + // HOLD에 있으면
				"    redis.call('SMOVE', KEYS[1], KEYS[2], seat) " + // HOLD -> BOOKED
				"    redis.call('HSET', KEYS[3], seat, 'BOOKED') " + // 상태 BOOKED
				"    count = count + 1 " + "  end " + "end " + "return count";

		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(luaScript);
		redisScript.setResultType(Long.class);

		return redisTemplate.execute(redisScript, List.of(holdSetKey, bookedSetKey, hashKey),
				seatNames.toArray(new String[0]));
	}

	// 좌석 해제 처리
	// [Set] HOLD/BOOKED 셋에서 제거 -> AVAILABLE 셋에 추가
	// [Hash] AVAILABLE로 업데이트
	// 좌석 해제 처리 (여러 좌석 한꺼번에 원자적 실행)
	public Long releaseSeats(int eventId, int scheduleId, List<String> seatNames) {
		String hashKey = buildSeatsKey(eventId, scheduleId);
		String availableSetKey = buildAvailableSeatsKey(eventId, scheduleId);
		String holdSetKey = buildHoldSeatsKey(eventId, scheduleId);
		String bookedSetKey = buildBookedSeatsKey(eventId, scheduleId);

		String luaScript = "local count = 0 " + "for i, seat in ipairs(ARGV) do "
				+ "  redis.call('SREM', KEYS[2], seat) " + // HOLD에서 제거
				"  redis.call('SREM', KEYS[3], seat) " + // BOOKED에서 제거
				"  redis.call('SADD', KEYS[1], seat) " + // AVAILABLE에 추가
				"  redis.call('HSET', KEYS[4], seat, 'AVAILABLE') " + // Hash 갱신
				"  count = count + 1 " + "end " + "return count";

		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(luaScript);
		redisScript.setResultType(Long.class);

		return redisTemplate.execute(redisScript, List.of(availableSetKey, holdSetKey, bookedSetKey, hashKey),
				seatNames.toArray(new String[0]));
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
