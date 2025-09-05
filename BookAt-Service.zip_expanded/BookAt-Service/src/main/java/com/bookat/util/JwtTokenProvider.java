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
    private final long expiration_30m = TimeUnit.MINUTES.toMillis(30); 	// 30분
    private final long expiration_7d = TimeUnit.DAYS.toMillis(7); 		// 7일
    
    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

	public String generateAccessToken(String userId) {
		return Jwts.builder()
				.setSubject(userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15)))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}
	
	public String generateRefreshToken(String userId) {
		return Jwts.builder()
				.setSubject(userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(50)))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}
	
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
	
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build()
				.parseClaimsJws(token);
			return true;
		} catch (JwtException |  IllegalArgumentException e) {
			return false;
		}
	}
	
//	public boolean validateToken(String token, String userId) {
//	    try {
//	        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
//	            .parseClaimsJws(token).getBody();
//	        String tokenUserId = claims.getSubject(); // 토큰에 담긴 userId
//	        return tokenUserId.equals(userId);       // DB와 비교
//	    } catch (JwtException | IllegalArgumentException e) {
//	        return false;
//	    }
//	}
}
