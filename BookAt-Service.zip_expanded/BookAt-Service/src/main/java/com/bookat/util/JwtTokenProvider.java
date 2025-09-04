package com.bookat.util;

import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.*;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
	
	private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expiration_30m = TimeUnit.MINUTES.toMillis(30); 			// 30분
    private final long expiration_7d = TimeUnit.DAYS.toMillis(7); 	// 7일

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
		return Jwts.parserBuilder().setSigningKey(key).build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();
	}
	
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build()
				.parseClaimsJws(token);
			return true;
		} catch (JwtException |  IllegalArgumentException e) {
			return false;
		}
	}
}
