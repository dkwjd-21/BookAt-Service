package com.bookat.util;

import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bookat.dto.PaymentSession;
import com.bookat.dto.reservation.PaymentReservationSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
  
	// 이벤트 결제 세션 토큰 관련
	public String createEventPay(PaymentReservationSession session) {
		try {
			String token = UUID.randomUUID().toString();
			String key = KEY_PREFIX + token;
			log.info("createEventPayToken userId : {}", session.userId());
			
			// 대량 트래픽에서도 Hash가 JSON 문자열 한 덩어리보다 부분 업데이트/조회가 가능
			Map<String,String> paymentData = new java.util.LinkedHashMap<>();
			paymentData.put("reservationToken", session.reservationToken());
			paymentData.put("eventId", String.valueOf(session.eventId()));
			paymentData.put("scheduleId", String.valueOf(session.scheduleId()));
			paymentData.put("method", (session.method() == null || session.method().isBlank()) ? "CARD" : session.method());
			paymentData.put("amount", session.amount().toPlainString());
			paymentData.put("merchantUid", session.merchantUid());
			paymentData.put("userId", session.userId());
			paymentData.put("status", "READY");
			paymentData.put("createdAt", java.time.Instant.now().toString());
			
			redis.opsForHash().putAll(key, paymentData);
			redis.expire(key, TTL);
		  
			return token;
		} catch (Exception e) {
			throw new RuntimeException("Payment session create failed", e);
		}
	}
	
	public Map<String, String> getEventPay(String token) {
		String key = KEY_PREFIX + token;
		Map<Object, Object> paymentData = redis.opsForHash().entries(key);
		if (paymentData == null || paymentData.isEmpty()) return null;
		var fields = List.of("reservationToken", "eventId", "scheduleId", "method", "amount", "merchantUid", "userId", "status", "createdAt");
		var vals   = redis.opsForHash().multiGet(key, new ArrayList<>(fields));
		if (vals == null || vals.stream().allMatch(Objects::isNull)) return null;
		Map<String, String> data = new HashMap<>();
		for (int i = 0; i < fields.size(); i++) {
			var v = vals.get(i);
			if (v != null) data.put(fields.get(i), v.toString());
		}
		
		return data;
	}
	
	public void consumeEventPay(String paymentToken) {
		redis.delete(KEY_PREFIX + paymentToken);
		
		// 또는 상태만 변경 후 짧은 TTL로 보관(디버깅/추적용)
		// 소비 타이밍: consumeEventPay()는 결제 검증/DB 반영 성공 직후에만 호출
		// redis.opsForHash().put(key, "status", "CLOSED");
		// redis.expire(key, java.time.Duration.ofMinutes(5));
	}
	
	private static boolean isBlank(String str) { return str == null || str.trim().isEmpty(); }
	
	public static PaymentReservationSession of(String reservationToken, int eventId, int scheduleId, String method, java.math.BigDecimal amount, String merchantUid, String userId) {
	          
		return new PaymentReservationSession(reservationToken, eventId, scheduleId, method, amount, merchantUid, userId, "READY", OffsetDateTime.now().toString());
	}
  
}