package com.bookat.service;

import org.springframework.stereotype.Service;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;

@Service
public interface UserLoginService {
	
	UserLoginResponse login(UserLoginRequest userLogin);
	User findUserById(String userId);
	User findPwByIdPhone(String userId, String phone);
	void updateRefreshToken(String refreshToken, String userId);
	void updatePassword(String password, String userId);

}
