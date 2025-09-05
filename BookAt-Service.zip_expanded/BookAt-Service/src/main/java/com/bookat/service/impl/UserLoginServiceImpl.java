package com.bookat.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.exception.LoginException;
import com.bookat.mapper.UserMapper;
import com.bookat.service.UserLoginService;
import com.bookat.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserLoginServiceImpl implements UserLoginService {
	
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	
	@Override
	public UserLoginResponse login (UserLoginRequest userLoginRequest) {
		
		User user = userMapper.findUserById(userLoginRequest.getUserId());
		
		if(user == null) {
			throw new LoginException("존재하지 않는 아이디입니다.");
		}
		
//		if(!passwordEncoder.matches(userLoginRequest.getUserPw(), user.getUserPw())) {
//			throw new LoginException("비밀번호가 일치하지 않습니다.");
//		}
		if(!user.getUserPw().equals(userLoginRequest.getUserPw())) {
			throw new LoginException("비밀번호가 일치하지 않습니다.");
		}
		
		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
		
//		user.setRefreshToken(refreshToken);
		
//		Map<String, String> values = new HashMap<>();
//		values.put("refreshToken", user.getRefreshToken());
//		values.put("userId", user.getUserId());
//		userMapper.updateUserRefreshToken(values);
		
		return new UserLoginResponse(accessToken, refreshToken);
	}
	
	@Override
	public User findUserById(String userId) {
		return userMapper.findUserById(userId);
	}
	
	@Override
	public void refreshTokenUpdate(String refreshToken, String userId) {
		Map<String, String> values = new HashMap<>();
		values.put("refreshToken", refreshToken);
		values.put("userId", userId);
		userMapper.updateUserRefreshToken(values);
	}
	
	// JWT 토큰 검증 (DB 조회 최소화)
	public boolean verifyToken(String token) {
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) return false;

        User user = userMapper.findUserById(userId); // 필요 시에만 DB 조회
        if (user == null) return false;

        return jwtTokenProvider.validateToken(token, user.getUserId());
    }

}
