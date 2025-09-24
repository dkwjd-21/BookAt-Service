package com.bookat.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.bookat.dto.PaymentSession;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentSessionStore {

    private static final String KEY_PREFIX = "payment:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redis;

    /** ✅ 권장: 이벤트/주문 공통 필드 “전부” Hash에 저장 */
    public String create(Map<String, Object> fields) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + token;

        Map<String, Object> map = new HashMap<>();
        if (fields != null) map.putAll(fields);

        // 필수 기본값 방어
        map.putIfAbsent("status", "READY");
        map.putIfAbsent("method", "CARD");
        map.putIfAbsent("impUid", ""); // 결제 전엔 비워둠

        redis.opsForHash().putAll(key, map);
        redis.expire(key, TTL);
        return token;
    }

    /** (옵션) 과거 흐름 호환용: record → Hash 저장 */
    public String create(PaymentSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("bookId", s.bookId());
        m.put("qty", String.valueOf(s.qty()));
        m.put("method", s.method());
        m.put("amount", s.amount().toPlainString());
        m.put("merchantUid", s.merchantUid());
        m.put("userId", s.userId());
        m.put("status", s.status());
        m.put("createdAt", s.createdAt());
        m.put("title", s.title());
        return create(m);
    }

    /** take=false: 조회만 / take=true: 조회 후 삭제 */
    public Map<Object, Object> getRaw(String token, boolean take) {
        String key = KEY_PREFIX + token;
        Map<Object, Object> h = redis.opsForHash().entries(key);
        if (h != null && !h.isEmpty()) {
            if (take) redis.delete(key);
            return h;
        }
        return null;
    }

    public void delete(String token) {
        redis.delete(KEY_PREFIX + token);
    }
}
