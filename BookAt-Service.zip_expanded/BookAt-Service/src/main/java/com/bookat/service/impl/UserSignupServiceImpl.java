package com.bookat.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookat.dto.UserSignup;
import com.bookat.mapper.UserSignupMapper;
import com.bookat.service.UserSignupService;

@Service
public class UserSignupServiceImpl implements UserSignupService{

	// 매퍼 
	@Autowired
	private UserSignupMapper mapper;
	
	@Override
	public UserSignup getUserById(String userId) {
		return mapper.selectUserById(userId);
	}

	@Override
	public UserSignup getUserByEmail(String email) {
		return mapper.selectUserByEmail(email);
	}

}
