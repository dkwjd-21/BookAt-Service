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
    	System.out.println("JWT Secret: " + secret);
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
				.setExpiration(new Date(System.currentTimeMillis() + expiration_7d))
				.signWith(key, SignatureAlgorithm.HS256)
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
