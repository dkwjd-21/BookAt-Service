package com.bookat.scheduler;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseWarmup {
	
	private final JdbcTemplate jdbcTemplate;
	
	@PostConstruct	// 애플리케이션 시작과 동시에 실행
	public void warmup() {
		// 간단한 쿼리로 커넥션 미리 확보
		jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
		System.out.println("DB Warm-up 완료!");
	}
}
