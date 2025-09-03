package com.bookat.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookat.entity.UserLogin;
import com.bookat.mapper.UserMapper;
import com.bookat.service.UserLoginService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserLoginServiceImpl implements UserLoginService {
	
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	
	public UserLogin login (UserLogin userLogin) {
		
		UserLogin user = userMapper.findUserById(userLogin.getUserId());
		
		if(user == null) {
			log.info("해당 유저 없음");
		}
		
		if(!passwordEncoder.matches(userLogin.getUserPw(), user.getUserPw())) {
			log.info("비밀번호가 일치하지 않음");
		}
		
		return user;
	}

}
