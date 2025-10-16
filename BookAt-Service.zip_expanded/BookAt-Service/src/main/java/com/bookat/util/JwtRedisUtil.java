package com.bookat.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRedisUtil {

	private final StringRedisTemplate redisTemplate;
	private static final String SID_KEY = "user:%s:current_sid";
	
	// SID (session id) 저장
	public void saveSid(String userId, String sid) {
		String sidKey = String.format(SID_KEY, userId);
		
		String luaScript = 
				"local old = redis.call('GET', KEYS[1]) " +
				"redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
				"return old";
	
		DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(luaScript);
		redisScript.setResultType(String.class);
		
//		redisTemplate.execute(redisScript, List.of(sidKey), sid, String.valueOf(JwtTokenProvider.EXPIRATION_1D));
		redisTemplate.execute(redisScript, List.of(sidKey), sid, String.valueOf(70*1000));
	}
	
	// 현재 session id 조회
	public String getCurrentSid(String userId) {
		String sidKey = String.format(SID_KEY, userId);
		return redisTemplate.opsForValue().get(sidKey);
	}
	
	// session id 삭제
	public void deleteSid(String userId) {
		String sidKey = String.format(SID_KEY, userId);
		redisTemplate.delete(sidKey);
	}
	
	// refresh token + loginTime 저장
	public void storeRefreshToken(String userId, String refreshToken, String loginTime) {
		Map<String, String> values = new HashMap<>();
		values.put("refreshToken", refreshToken);
		values.put("loginTime", loginTime);
		
		redisTemplate.opsForHash().putAll(userId, values);
//		redisTemplate.expire(userId, JwtTokenProvider.EXPIRATION_1D, TimeUnit.MILLISECONDS);
		redisTemplate.expire(userId, 70*1000, TimeUnit.MILLISECONDS);
	}
	
	// refresh token + loginTime 조회
	public Map<String, String> getRefreshTokenInfo(String userId) {
		Map<Object, Object> redisData = redisTemplate.opsForHash().entries(userId);
		Map<String, String> result = new HashMap<>();
		
		if(redisData != null && !redisData.isEmpty()) {
			redisData.forEach((k, v) -> result.put((String) k, (String) v));
		}
		
		return result;
	}
	
	// refresh token + loginTime 삭제
	public void deleteRefreshToken(String userId) {
		redisTemplate.delete(userId);
	}
}
