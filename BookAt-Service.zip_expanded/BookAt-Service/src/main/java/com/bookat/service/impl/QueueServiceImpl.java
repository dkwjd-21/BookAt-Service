package com.bookat.service.impl;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bookat.controller.OrderController;
import com.bookat.service.QueueService;

@Service
public class QueueServiceImpl implements QueueService{

    private final OrderController orderController;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	// 이벤트 대기열 Redis 키 네이밍 규칙 
	private static final String QUEUE_KEY_PREFIX = "EVENT:QUEUE:";
	// 이벤트 대기열의 마지막 heartbeat 저장용 Hash 네이밍 
	private static final String HEARTBEAT_PREFIX = "EVENT:HEARTBEAT:";
	
	// 예매 진행중 인원 Redis 키 네이밍 규칙 
	private static final String ACTIVE_KEY_PREFIX = "EVENT:ACTIVE:";

    QueueServiceImpl(OrderController orderController) {
        this.orderController = orderController;
    }
		
	@Override
	// 사용자를 이벤트 대기열에 추가한다. 
	public Long addUserToQueue(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		
		long now = System.currentTimeMillis();
		
		// 현재 시간을 사용하여 Sorted Set(ZSet)에 추가한다.
		// 먼저 들어온 사람이 더 낮은 Rank를 가진다. 
		redisTemplate.opsForZSet().add(queueKey, userId, now);
		
		// heartbeat Hash에 기록
		redisTemplate.opsForZSet().add(heartbeatKey, userId, now);
		
		// ZSet에서의 랭크는 0부터 시작한다. -> +1 
		return redisTemplate.opsForZSet().rank(queueKey, userId) +1;
	}

	@Override
	// 사용자의 대기 순번을 조회한다. 
	public Long getUserRank(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		
		Long rank = redisTemplate.opsForZSet().rank(queueKey, userId);
		
		// 랭크가 null이 아니면 +1 해서 반환
		return (rank != null)? rank+1 : null;
	}

	@Override
	// 사용자를 이벤트 대기열에서 삭제한다. 
	public boolean leaveQueue(String eventId, String userId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		
		Long removed = redisTemplate.opsForZSet().remove(queueKey, userId);
		redisTemplate.opsForZSet().remove(heartbeatKey, userId);
		
		return removed!=null && removed > 0;
	}

	@Override
	// 사용자를 예매진입창에 추가한다. 
	public boolean tryEnterActive(String eventId, String userId) {
		// Redis activeKey
		String activeKey = ACTIVE_KEY_PREFIX + eventId;
		
		if(canEnterReservation(eventId, userId)) {
			// 아직 자리가 있으면 추가
			Long added = redisTemplate.opsForSet().add(activeKey, userId);
			System.out.println("added : "+added);
			
			if(added != null && added > 0) {
				// 입장 성공 시 대기열에서 제거 
				leaveQueue(eventId, userId);
				return true;
			} else {
				return false;
			}
		} else {
			// 인원이 가득 찼으면 실패 
			return false;
		}
	}

	@Override
	// 예매창에서 나가기
	public void leaveActive(String eventId, String userId) {
		String activeKey = ACTIVE_KEY_PREFIX + eventId;
		redisTemplate.opsForSet().remove(activeKey, userId);
	}

	@Override
	// 현재 예매창에 들어와 있는 인원 수 확인
	public Long getActiveCount(String eventId) {
		String activeKey = ACTIVE_KEY_PREFIX + eventId;
		return redisTemplate.opsForSet().size(activeKey);
	}

	@Override
	// 사용자가 예매창에 들어갈 수 있는지 확인 
	public boolean canEnterReservation(String eventId, String userId) {
		Long rank = getUserRank(eventId, userId);
		if(rank == null) return false;
		
		// 현재 예매 진행중인 인원
		Long currentActive = getActiveCount(eventId);
		if(currentActive == null) currentActive = 0L;
		
		int MAX_ACTIVE = 1; 	// 동시 예매 허용 인원
		return (rank == 1 && currentActive < MAX_ACTIVE);
	}

	@Override
	// 현재 대기열의 크기 
	public Long getQueueCount(String eventId) {
		String queueKey = QUEUE_KEY_PREFIX + eventId;
		Long size = redisTemplate.opsForZSet().size(queueKey);
		return size != null ? size : 0L;
	}

	@Override
	// heartbeat(마지막 폴링 시각) 갱신
	public void updateLastActive(String eventId, String userId) {
		String heartbeatKey = HEARTBEAT_PREFIX + eventId;
		redisTemplate.opsForZSet().add(heartbeatKey, userId, System.currentTimeMillis());
	}	
	
	// -------------------- 자동 제거 --------------------
	@Scheduled(fixedRate = 5000) // 5초마다 실행
	public void removeInactiveUsers() {
	    Set<String> queueKeys = redisTemplate.keys(QUEUE_KEY_PREFIX + "*");
	    if (queueKeys == null || queueKeys.isEmpty()) return;

	    long now = System.currentTimeMillis();
	    long threshold = now - 10_000; // 10초 이상 폴링 없는 사용자

	    for (String queueKey : queueKeys) {
	        String eventId = queueKey.substring(QUEUE_KEY_PREFIX.length());
	        String heartbeatKey = HEARTBEAT_PREFIX + eventId;

	        // heartbeat ZSet에서 inactivity threshold 기준 조회
	        Set<String> inactiveUsers = redisTemplate.opsForZSet()
	                                                .rangeByScore(heartbeatKey, 0, threshold);
	        if (inactiveUsers == null || inactiveUsers.isEmpty()) continue;

	        for (String userId : inactiveUsers) {
	            // queueKey(ZSet)에서 순번 유지하며 삭제
	            redisTemplate.opsForZSet().remove(queueKey, userId);
	            // heartbeat ZSet에서도 삭제
	            redisTemplate.opsForZSet().remove(heartbeatKey, userId);
	            System.out.println("자동 제거: eventId=" + eventId + ", userId=" + userId);
	        }
	    }
	}


}
