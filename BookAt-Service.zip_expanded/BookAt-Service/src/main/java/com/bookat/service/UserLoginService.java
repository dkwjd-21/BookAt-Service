package com.bookat.service;

import org.springframework.stereotype.Service;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;

@Service
public interface UserLoginService {
	
	UserLoginResponse login(UserLoginRequest userLogin);

}
