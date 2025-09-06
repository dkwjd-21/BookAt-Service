package com.bookat.service;

import com.bookat.dto.UserSignup;

public interface UserSignupService {
	UserSignup getUserById(String userId);
	UserSignup getUserByEmail(String email);
	int insertUser(UserSignup user);
}