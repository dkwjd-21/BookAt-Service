package com.bookat.util;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWarmRunner implements ApplicationRunner {
	
	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		try {
			redisTemplate.opsForValue().set("warmup:key", "1");
			redisTemplate.opsForValue().get("warmup:key");
		} catch(Exception e) {
			log.warn("Redis Warm-up Failed : {}", e.getMessage());
		}
	}
}
