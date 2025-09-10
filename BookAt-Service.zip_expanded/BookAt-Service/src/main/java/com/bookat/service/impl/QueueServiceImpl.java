package com.bookat.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.bookat.service.QueueService;

@Service
public class QueueServiceImpl implements QueueService{

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Override
	public Long addUserToQueue(String eventId, String userId) {
		
		return null;
	}

	@Override
	public Long getUserRank(String eventId, String userId) {
		
		return null;
	}

}
