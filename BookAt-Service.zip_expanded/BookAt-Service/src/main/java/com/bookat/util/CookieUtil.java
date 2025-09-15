package com.bookat.util;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {
	
    public static final int ONE_MINUTE = (int) TimeUnit.MINUTES.toSeconds(1);
    public static final int THIRTY_MINUTES = (int) TimeUnit.MINUTES.toSeconds(30);
//    public static final int ONE_HOUR = (int) TimeUnit.HOURS.toSeconds(1);
//    public static final int ONE_DAY = (int) TimeUnit.DAYS.toSeconds(1);
    public static final int SEVEN_DAYS = (int) TimeUnit.DAYS.toSeconds(7);

	// 쿠키 생성
    public void createCookie(HttpServletResponse response, String key, String value, int maxAgeSeconds) {
    	Cookie cookie  = new Cookie(key, value);
		cookie .setHttpOnly(true);
//		cookie .setSecure(true);
		cookie .setPath("/");
		cookie.setMaxAge(maxAgeSeconds);
		response.addCookie(cookie);
    }
    
    // 쿠키 값 반환
    public String getCookieValue(HttpServletRequest request, String cookieName) {
    	if (request.getCookies() != null) {
    		for (Cookie cookie : request.getCookies()) {
    			if (cookie.getName().equals(cookieName)) {
    				return cookie.getValue();
    			}
    		}
    	}
    	return null;
    }
    
    // 브라우저에서 쿠키 삭제
    public void deleteCookie(HttpServletResponse response, String cookieName) {
    	Cookie cookie = new Cookie(cookieName, null);
    	cookie.setHttpOnly(true);
    	cookie.setMaxAge(0);
    	cookie.setPath("/");
    	response.addCookie(cookie);
    }
	
}
