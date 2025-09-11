package com.bookat.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.bookat.service.QueueService;

@Service
public class QueueServiceImpl implements QueueService{

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	// 이벤트 대기열 Redis 키 네이밍 규칙 
	private static final String QUEUE_KEY_PREFIX = "event:queue:";
	
	@Override
	// 사용자를 이벤트 대기열에 추가한다. 
	public Long addUserToQueue(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		
		// 현재 시간을 사용하여 Sorted Set(ZSet)에 추가한다.
		// 먼저 들어온 사람이 더 낮은 Rank를 가진다. 
		redisTemplate.opsForZSet().add(queueKey, userId, System.currentTimeMillis());
		
		// ZSet에서의 랭크는 0부터 시작한다. -> +1 
		return redisTemplate.opsForZSet().rank(queueKey, userId) +1;
	}

	@Override
	// 사용자의 대기 순번을 조회한다. 
	public Long getUserRank(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		
		Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
		
		// 랭크가 null이 아니면 +1 해서 반환
		return (rank != null)? rank+1 : null;
	}

}
