package com.bookat.service;

import com.bookat.dto.UserSignup;

public interface UserSignupService {
	UserSignup getUserById(String userId);
	UserSignup getUserByEmail(String email);
	UserSignup getUserByPhone(String phone);
	int insertUser(UserSignup user);
}