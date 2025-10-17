package com.bookat.config;

import java.util.Map;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisExpirationListener implements MessageListener {
	
	// TTL 만료 시 좌석 복구
	
	private final StringRedisTemplate redisTemplate;
	
	public RedisExpirationListener(StringRedisTemplate redisTemplate, RedisMessageListenerContainer container) {
		this.redisTemplate = redisTemplate;
		container.addMessageListener(this, new PatternTopic("__keyevent@0__:expired"));
	}
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		log.info("만료된 key 감지 : {}", expiredKey);
		
		if(expiredKey.startsWith("RESERVATION:")) {
			String token = expiredKey.replace("RESERVATION:", "");
			recoverSeatsFromMeta(token);
		}
		
	}

	// TTL 만료로 예약키가 날아갔을 때만 동작
	// 예약키 대신 META 키에서 데이터 꺼내서 좌석 복구 → META 키 삭제
	private void recoverSeatsFromMeta(String reservationToken) {
		String metaKey = "RESERVATION_META:" + reservationToken;
		Map<Object, Object> data = redisTemplate.opsForHash().entries(metaKey);
		
		if(data.isEmpty()) {
			log.warn("META 데이터 없음, 좌석 복구 불가: {}", reservationToken);
			return;
		}
		
		String eventId = (String) data.get("eventId");
		String scheduleId = (String) data.get("scheduleId");
		int reservedCount = Integer.parseInt((String) data.get("reservedCount"));
		
		if(reservedCount > 0) {
			String availableSeatsKey = String.format("EVENT:%s:SCHEDULE:%s:AVAILABLE_SEAT", eventId, scheduleId);
			redisTemplate.opsForValue().increment(availableSeatsKey, reservedCount);
			log.info("TTL 만료, 좌석 자동 복구 완료: {}석 (eventId={}, scheduleId={})", reservedCount, eventId, scheduleId);
		}
		
		// 원자적 처리 예정
//		int recovered = redisUtil.rollbackOnCancel(reservationToken, eventId, scheduleId);
		
		// TTL 만료 후 metaData도 정리
		redisTemplate.delete(metaKey);
	}
	
}
