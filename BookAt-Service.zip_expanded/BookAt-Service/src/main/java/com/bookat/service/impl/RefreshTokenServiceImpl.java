package com.bookat.service.impl;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

import com.bookat.service.RefreshTokenService;
import com.bookat.util.CookieUtil;
import com.bookat.util.JwtRedisUtil;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
	
	private final JwtTokenProvider jwtTokenProvider;
	private final CookieUtil cookieUtil;
	private final JwtRedisUtil jwtRedisUtil;
	
	@Override
	public boolean validateRefreshToken(HttpServletRequest request, HttpServletResponse response, String userId) {
		
		String refreshToken = cookieUtil.getCookieValue(request, "refreshToken");
		String loginTime = cookieUtil.getCookieValue(request, "loginTime");
		
		Map<String, String> redisValue = jwtRedisUtil.getRefreshTokenInfo(userId);
		
		if(redisValue == null || redisValue.isEmpty()) {
			log.info("redis 에 저장된 세션이 없음");
			return false;
		}
		
		String redisRefreshToken = redisValue.get("refreshToken");
		String redisLoginTime = redisValue.get("loginTime");
		
		// refresh token 만료 여부 체크
		if(redisRefreshToken != null && !jwtTokenProvider.validateToken(redisRefreshToken)) {
			log.info("refresh token 만료, redis 삭제 필요");
			return false;
		}
		
		// 동시 로그인 체크
		if(!Objects.equals(refreshToken, redisRefreshToken) || !Objects.equals(loginTime, redisLoginTime)) {
			log.info("다른 기기에서 로그인, 현재 기기 강제 로그아웃 필요");
			return false;
		}
		
		return true;
	}

	@Override
	public void storeRefreshToken(String userId, String refreshToken, String loginTime) {
		jwtRedisUtil.storeRefreshToken(userId, refreshToken, loginTime);
	}

}
