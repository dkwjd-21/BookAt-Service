package com.bookat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedisConfig {

	  @Value("${spring.redis.host}")
	  private String redisHost;
	 
	  @Value("${spring.redis.port}")
	  private String redisPort;
	 
	  @Value("${spring.redis.password}")
	  private String redisPassword;
	  
	  @Bean
	  public RedisConnectionFactory redisConnectionFactory() {
		  
		  RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		  redisStandaloneConfiguration.setHostName(redisHost);
		  redisStandaloneConfiguration.setPort(Integer.parseInt(redisPort));
		  if(redisPassword != null && !redisPassword.isEmpty()) {
			  redisStandaloneConfiguration.setPassword(redisPassword);
		  }
		  LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration);
		  
		  return lettuceConnectionFactory;
		  
	  }
	  
	  @Bean
	  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		  RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		  redisTemplate.setConnectionFactory(redisConnectionFactory);
		  
		  // String 직렬화
		  redisTemplate.setKeySerializer(new StringRedisSerializer());
		  redisTemplate.setValueSerializer(new StringRedisSerializer());
		  redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		  redisTemplate.setHashValueSerializer(new StringRedisSerializer());
		  
		  redisTemplate.afterPropertiesSet();
		  
		  return redisTemplate;
	  }
	  
}
