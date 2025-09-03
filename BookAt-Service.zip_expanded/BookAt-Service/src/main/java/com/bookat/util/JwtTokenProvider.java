package com.bookat.util;

import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.*;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
	
	private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expiration_30m = 1000L * 60 * 30; 			// 30분
    private final long expiration_7d = 1000L * 60 * 60 * 24 * 7; 	// 7일

	public String generateAccessToken(String userId) {
		return Jwts.builder()
				.setSubject(userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration_30m))
				.signWith(key)
				.compact();
	}
	
	public String generateRefreshToken(String userId) {
		return Jwts.builder()
				.setSubject(userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration_7d))
				.signWith(key)
				.compact();
	}
	
	public String getUserIdFromToken(String token) {
		return "";
	}
	
	public String validateToken(String token) {
		return "";
	}
}
