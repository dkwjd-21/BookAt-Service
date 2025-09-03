package com.bookat.service;

import org.springframework.stereotype.Service;

import com.bookat.entity.UserLogin;

@Service
public interface UserLoginService {
	
	UserLogin login(UserLogin userLogin);

}
