package com.bookat.util;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CaptchaStore {

	private final StringRedisTemplate redisTemplate;
	
	public CaptchaStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}
	
	// 저장 (TTL 3분)
	public void save(String captchaId, String answer) {
		redisTemplate.opsForValue().set(captchaId, answer, 3, TimeUnit.MINUTES);
	}
	
	// 조회
	public String get(String captchaId) {
		return redisTemplate.opsForValue().get(captchaId);
	}
	
	// 삭제
	public void remove(String captchaId) {
		redisTemplate.delete(captchaId);
	}
	
}
