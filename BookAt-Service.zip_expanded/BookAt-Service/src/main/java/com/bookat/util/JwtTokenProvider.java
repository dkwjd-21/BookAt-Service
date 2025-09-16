package com.bookat.util;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.*;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
	
	@Value("${jwt.secret}")
    private String secret;
    private Key key;
    private final long expiration_30m = TimeUnit.MINUTES.toMillis(30); 	// 30분 (access token)
    private final long expiration_7d = TimeUnit.DAYS.toMillis(7); 		// 7일 (refresh token)
    
    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // access token 생성
	public String generateAccessToken(String userId) {
		return Jwts.builder()
				.setSubject(userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration_30m))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}
	
	// refresh token 생성
	public String generateRefreshToken(String userId) {
		return Jwts.builder()
				.setSubject(userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration_7d))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}
	
	// token 으로 userId 검증
	public String getUserIdFromToken(String token) {
	    try {
	        return Jwts.parserBuilder()
	                   .setSigningKey(key)
	                   .build()
	                   .parseClaimsJws(token)
	                   .getBody()
	                   .getSubject();
	    } catch (ExpiredJwtException e) {
	        // 만료된 토큰에서도 Claims 추출 가능
	        return e.getClaims().getSubject();
	    }
	}
	
	// token 유효성 검사
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
