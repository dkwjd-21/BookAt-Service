package com.bookat.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bookat.domain.PaymentStatus;
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
          String title,Long orderId) {
          
	     return new PaymentSession(bookId, qty, method, amount, merchantUid, userId, "READY", OffsetDateTime.now().toString(),title,orderId
         );
  }
 
  
	// === 이벤트 결제 세션 토큰 관련 ===
	public String createEventPay(PaymentReservationSession session) {
		try {
			String token = UUID.randomUUID().toString();
			String key = KEY_PREFIX + token;
			
			// 대량 트래픽에서도 hash 구조가 JSON 문자열 한 덩어리보다 부분 업데이트/조회가 가능
			Map<String,String> paymentData = new LinkedHashMap<>();
			paymentData.put("reservationToken", session.reservationToken());
			paymentData.put("eventId", String.valueOf(session.eventId()));
			paymentData.put("scheduleId", String.valueOf(session.scheduleId()));
			paymentData.put("title", session.title());
			paymentData.put("reservedCount", String.valueOf(session.reservedCount()));
			paymentData.put("method", (session.method() == null || session.method().isBlank()) ? "CARD" : session.method());
			paymentData.put("amount", session.amount().toPlainString());
			paymentData.put("merchantUid", session.merchantUid());
			paymentData.put("impUid", session.impUid());
			paymentData.put("userId", session.userId());
			paymentData.put("status", session.status().name());
			paymentData.put("createdAt", session.createdAt().toString());
			
			redis.opsForHash().putAll(key, paymentData);
			redis.expire(key, TTL);
		  
			return token;
		} catch (Exception e) {
			throw new RuntimeException("Payment session create failed", e);
		}
	}
	
	public PaymentReservationSession getEventPay(String token) {
		String key = KEY_PREFIX + token;
		Map<Object, Object> paymentData = redis.opsForHash().entries(key);
		
		if (paymentData == null || paymentData.isEmpty()) return null;
		
		return new PaymentReservationSession(
				(String) paymentData.get("reservationToken"),
				Integer.parseInt((String) paymentData.get("eventId")),
				Integer.parseInt((String) paymentData.get("scheduleId")),
				(String) paymentData.get("title"),
				Integer.parseInt((String) paymentData.get("reservedCount")),
				(String) paymentData.get("method"),
				new BigDecimal((String) paymentData.get("amount")),
				(String) paymentData.get("merchantUid"),
				(String) paymentData.get("impUid"),
				(String) paymentData.get("userId"),
				PaymentStatus.valueOf((String) paymentData.get("status")),
				LocalDateTime.parse((String) paymentData.get("createdAt"))
				);
	}
	
	// 결제 후 포트원 결제정보 및 결제상태 갱신
	public void updateImpUid(String token, String impUid) {
		String key = KEY_PREFIX + token;
		redis.opsForHash().put(key, "impUid", impUid);
		redis.opsForHash().put(key, "status", PaymentStatus.PAID.name());
	}
	
	// 세션 소비 (삭제)
	public void consumeEventPay(String paymentToken) {
		redis.delete(KEY_PREFIX + paymentToken);
		
		// 또는 상태만 변경 후 짧은 TTL로 보관(디버깅/추적용)
		// 소비 타이밍: consumeEventPay()는 결제 검증/DB 반영 성공 직후에만 호출
		// redis.opsForHash().put(key, "status", "CLOSED");
		// redis.expire(key, java.time.Duration.ofMinutes(5));
	}
	
	public static PaymentReservationSession of(String reservationToken, int eventId, int scheduleId, String title, int reservedCount, String method, BigDecimal amount, String merchantUid, String userId) {
		Instant now = Instant.now();
		ZoneId seoulZone = ZoneId.of("Asia/Seoul");
		LocalDateTime koreaTime = LocalDateTime.ofInstant(now, seoulZone);
		
		// impUid 는 결제 완료 후 세팅 : 포트원에서 결제가 실제로 발생했을 때 응답으로 부여하는 값
		return new PaymentReservationSession(reservationToken, eventId, scheduleId, title, reservedCount, method, amount, merchantUid, null, userId, PaymentStatus.READY, koreaTime);
	}
  
}
