package com.bookat.util;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bookat.dto.PaymentSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSessionStore {

  private final RedisTemplate<String, Object> redis;
  private final ObjectMapper om = new ObjectMapper();

  private static final String KEY_PREFIX = "payment:";
  private static final Duration TTL = Duration.ofMinutes(10);

  public String create(PaymentSession session) {
    try {
      String token = UUID.randomUUID().toString();
      String key = KEY_PREFIX + token;
      String json = om.writeValueAsString(session);
      redis.opsForValue().set(key, json, TTL);   
      return token;
    } catch (Exception e) {
      throw new RuntimeException("Payment session create failed", e);
    }
  }

  /** get: take=true → 1회용(읽고 즉시 삭제) */
  public PaymentSession get(String token, boolean take) {
    try {
      String key = KEY_PREFIX + token;
      Object v = redis.opsForValue().get(key);   // StringSerializer → String
      if (v == null) return null;
      if (take) redis.delete(key);
      return om.readValue(v.toString(), PaymentSession.class);
    } catch (Exception e) {
      return null;
    }
  }

  public void delete(String token) {redis.delete(KEY_PREFIX + token);
  }

  public static PaymentSession of(String bookId, int qty, String method,
          java.math.BigDecimal amount, String merchantUid, String userId,
          String title) {   // ← title 추가
          
	     return new PaymentSession(bookId, qty, method, amount, merchantUid, userId, "READY", OffsetDateTime.now().toString(),title
         );
  }
  
}