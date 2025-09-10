package com.bookat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
	
	// application.properties에서 설정한 정보 주입 
	@Value("${spring.redis.host}")
	private String redisHost;
	
	@Value("${spring.redis.port}")
	private String redisPort;
	
	// Redis 서버의 주소, 포트, 비밀번호 등 접속 정보 관리 
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		// Redis 서버의 접속 정보를 설정하는 객체 생성
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		
		// 접속 정보 설정
		redisStandaloneConfiguration.setHostName(redisHost);
		redisStandaloneConfiguration.setPort(Integer.parseInt(redisPort));
		
		// Lettuce 라이브러리를 사용한 연결 팩토리 생성 
		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration);
		
		return lettuceConnectionFactory;
	}
	
	
	// Redis 서버에 데이터를 저장 및 조회 등 실제 작업을 수행하는 RedisTemplate를 빈으로 등록
	// 데이터를 어떤 형식으로 저장할지 등을 설정
	@Bean
	public RedisTemplate<String, Object> redisTemplate(){
		// RedisTemplate 객체 생성
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		
		// redisConnectionFactory()를 사용하여 Redis 서버와 연결
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		
		// Key 값을 문자열로 설정
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		
		// Value 값을 문자열로 설정
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		
		return redisTemplate;
	}
}
