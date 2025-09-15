package com.bookat.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RefreshTokenService {

	public boolean validateRefreshToken(HttpServletRequest request, HttpServletResponse response, String userId);
	public void storeRefreshToken(String userId, String refreshToken, String loginTime, int storeTime);
	
}
