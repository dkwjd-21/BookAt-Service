package com.bookat.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
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
	
	public UserLoginResponse login (UserLoginRequest userLoginRequest) {
		
		log.info("userId : {}", userLoginRequest.getUserId());
		
		User user = userMapper.findUserById(userLoginRequest.getUserId());
		
		if(user == null) {
			log.info("해당 유저 없음");
			return null;
		}
		
//		if(!passwordEncoder.matches(userLoginRequest.getUserPw(), user.getUserPw())) {
//			log.info("비밀번호가 일치하지 않음");
//			return null;		
//		}
		
		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
		
		user.setRefreshToken(refreshToken);
		
		Map<String, String> values = new HashMap<>();
		values.put("refreshToken", user.getRefreshToken());
		values.put("userId", user.getUserId());
		userMapper.updateUserRefreshToken(values);
		
		return new UserLoginResponse(accessToken, refreshToken);
	}

}
