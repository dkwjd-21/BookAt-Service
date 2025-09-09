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
		
		if(!passwordEncoder.matches(userLoginRequest.getUserPw(), user.getUserPw())) {
			throw new LoginException("비밀번호가 일치하지 않습니다.");
		}
		
		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
		
//		user.setRefreshToken(refreshToken);
		
		// 리프레시토큰 디비 저장
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
	public User findPwByIdPhone(String userId, String phone) {
		User user = userMapper.findUserById(userId);
		
		if(user == null) {
			throw new LoginException("존재하지 않는 아이디입니다.");
		}
		
		String dbPhone = user.getPhone() != null ? user.getPhone().replaceAll("-", "") : "";
		
		if(!phone.equals(dbPhone)) {
			throw new LoginException("전화번호가 일치하지 않습니다.");
		}
		
		return user;
	}
	
	@Override
	public void updateRefreshToken(String refreshToken, String userId) {
		Map<String, String> values = new HashMap<>();
		values.put("refreshToken", refreshToken);
		values.put("userId", userId);
		userMapper.updateUserRefreshToken(values);
	}
	
	@Override
	public void updatePassword(String password, String userId) {
		Map<String, String> values = new HashMap<>();
		values.put("password", passwordEncoder.encode(password));
		values.put("userId", userId);
		userMapper.updatePassword(values);
	}
	
	@Override
	public User findIdBySimpleAuth(String userName, String phone, String birth) {

		if(phone != null && phone.length() == 11) {
			phone = phone.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
		}
		
		Map<String, String> values = new HashMap<>();
		values.put("userName", userName);
		values.put("phone", phone);
		values.put("birth", birth);
		User user = userMapper.findIdBySimpleAuth(values);
		
		if(user == null) {
			throw new LoginException("회원정보가 존재하지 않습니다.");
		}
		
		return user;
	}

}
