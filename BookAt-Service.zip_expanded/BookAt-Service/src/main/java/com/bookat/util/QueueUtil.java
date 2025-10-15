package com.bookat.util;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueueUtil {
	private final StringRedisTemplate redisTemplate;

	private static final String QUEUE_KEY_PREFIX = "EVENT:QUEUE:";
	private static final String HEARTBEAT_PREFIX = "EVENT:HEARTBEAT:";
	private static final String ACTIVE_KEY_PREFIX = "EVENT:ACTIVE:";
	// 예매변경을 위한 키
	private static final String MODIFICATION_ID_PREFIX = "EVENT:MODIFY_ID:";

	// 사용자를 큐에 추가 & heartbeat 기록
	public long addUserToQueue(String eventId, String userId, String reservationId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		String modifyIdKey = MODIFICATION_ID_PREFIX + eventId;
		long now = System.currentTimeMillis();

		redisTemplate.opsForZSet().add(queueKey, userId, now);
		redisTemplate.opsForZSet().add(heartbeatKey, userId, now);

		// 예약 변경 ID 저장 로직 추가
		if (reservationId != null && !reservationId.isEmpty()) {
			// Redis Hash: HSET EVENT:MODIFY_ID:{eventId} {userId} {reservationId}
			redisTemplate.opsForHash().put(modifyIdKey, userId, reservationId);
		}

		Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
		return rank != null ? rank + 1 : -1L;
	}

	// [오버로드] 기존 호출부를 위한 메서드 유지
	public long addUserToQueue(String eventId, String userId) {
		return addUserToQueue(eventId, userId, null);
	}

	// 사용자를 큐에서 제거 & heartbeat 제거
	public boolean removeFromQueue(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		String modifyIdKey = MODIFICATION_ID_PREFIX + eventId;

		Long removed = redisTemplate.opsForZSet().remove(queueKey, userId);
		redisTemplate.opsForZSet().remove(heartbeatKey, userId);

		// 💡 예약 변경 ID 제거 로직 추가
		redisTemplate.opsForHash().delete(modifyIdKey, userId);

		return removed != null && removed > 0;
	}

	// heartbeat 갱신
	public void updateHeartbeat(String eventId, String userId) {
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		redisTemplate.opsForZSet().add(heartbeatKey, userId, System.currentTimeMillis());
	}

	// 예매팝업창 입장 처리
	public boolean tryEnterActiveAtomic(String eventId, String userId, int maxActive) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		String activeKey = ACTIVE_KEY_PREFIX + eventId;
		String modifyIdKey = MODIFICATION_ID_PREFIX + eventId;

		String lua = """
				    local queueKey = KEYS[1]
				    local heartbeatKey = KEYS[2]
				    local activeKey = KEYS[3]
				    local modifyIdKey = KEYS[4]
				    local userId = ARGV[1]
				    local maxActive = tonumber(ARGV[2])

				    -- 현재 순번(rank) 확인
				    local rank = redis.call('ZRANK', queueKey, userId)
				    if not rank or tonumber(rank) ~= 0 then
				        return 0
				    end

				    -- 현재 active 인원 수 확인
				    local activeCount = redis.call('SCARD', activeKey)
				    if activeCount >= maxActive then
				        return 0
				    end

				    -- 원자적 처리: active 등록 & queue/heartbeat 제거
				    redis.call('SADD', activeKey, userId)
				    redis.call('ZREM', queueKey, userId)
				    redis.call('ZREM', heartbeatKey, userId)
				    redis.call('HDEL', modifyIdKey, userId)

				    return 1
				""";

		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(lua);
		script.setResultType(Long.class);

		Long result = redisTemplate.execute(script, java.util.List.of(queueKey, heartbeatKey, activeKey, modifyIdKey),
				userId, String.valueOf(maxActive));

		return result != null && result == 1L;
	}

	// 예매 팝업창에서 제거
	public void leaveActive(String eventId, String userId) {
		String activeKey = ACTIVE_KEY_PREFIX + eventId;
		redisTemplate.opsForSet().remove(activeKey, userId);
	}


	// 폴링하지 않는 사용자 제거 (Lua 스크립트 수정)
	public long removeInactiveUsersAtomic(String eventId, long threshold) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		String modifyIdKey = MODIFICATION_ID_PREFIX + eventId; // 💡 키 추가

		String lua = """
				    local queueKey = KEYS[1]
				    local heartbeatKey = KEYS[2]
				    local modifyIdKey = KEYS[3] 
				    local threshold = tonumber(ARGV[1])

				    local inactive = redis.call('ZRANGEBYSCORE', heartbeatKey, 0, threshold)
				    if #inactive == 0 then
				        return 0
				    end

				    for i, userId in ipairs(inactive) do
				        redis.call('ZREM', queueKey, userId)
				        redis.call('ZREM', heartbeatKey, userId)
				        redis.call('HDEL', modifyIdKey, userId) 
				    end

				    return #inactive
				""";

		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(lua);
		script.setResultType(Long.class);

		Long result = redisTemplate.execute(script, java.util.List.of(queueKey, heartbeatKey, modifyIdKey),
				String.valueOf(threshold));

		return result != null ? result : 0L;
	}

	// 현재 active(예약 진행 중인) 인원 수 조회
	public long getActiveCount(String eventId) {
		String activeKey = ACTIVE_KEY_PREFIX + eventId;
		Long size = redisTemplate.opsForSet().size(activeKey);
		return size != null ? size : 0L;
	}

	// 사용자의 현재 rank(대기 순번) 조회
	public Long getUserRank(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
		return rank != null ? rank + 1 : null;
	}

	// 현재 대기열의 크기 조회
	public long getQueueCount(String eventId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		Long size = redisTemplate.opsForZSet().size(queueKey);
		return size != null ? size : 0L;
	}

	// 현재 존재하는 모든 큐 키 조회
	public Set<String> getAllQueueKeys() {
		return redisTemplate.keys(QUEUE_KEY_PREFIX + "*");
	}

	// 큐 키에서 eventId 추출
	public String extractEventId(String queueKey) {
		return queueKey.substring(QUEUE_KEY_PREFIX.length());
	}
	
	// 사용자의 예약 변경 ID 조회
	public String getReservationId(String eventId, String userId) {
	    String modifyIdKey = MODIFICATION_ID_PREFIX + eventId;
	    Object reservationId = redisTemplate.opsForHash().get(modifyIdKey, userId);
	    return reservationId != null ? reservationId.toString() : null;
	}
}
