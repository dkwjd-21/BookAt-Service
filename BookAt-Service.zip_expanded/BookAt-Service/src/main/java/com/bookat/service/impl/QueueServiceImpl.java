package com.bookat.service.impl;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.bookat.service.QueueService;
import com.bookat.util.QueueUtil;

@Service
public class QueueServiceImpl implements QueueService{

    private QueueUtil queueUtil = null;
    // 동시 예매 허용 인원
    private static final int MAX_ACTIVE = 1;
    
    public QueueServiceImpl(QueueUtil queueUtil) {
        this.queueUtil = queueUtil;
    }
    
    @Override
    // 이벤트 대기열에 추가 & heartbeat 업데이트
    public Long addUserToQueue(String eventId, String userId) {
        return queueUtil.addUserToQueue(eventId, userId);
    }

    @Override
    // 사용자의 대기 순번 조회
    public Long getUserRank(String eventId, String userId) {
        return queueUtil.getUserRank(eventId, userId);
    }

    @Override
    // 이벤트 대기열에서 제거 
    public boolean leaveQueue(String eventId, String userId) {
        return queueUtil.removeFromQueue(eventId, userId);
    }

    @Override
    // 예약 팝업창 입장 시도 
    public boolean tryEnterActive(String eventId, String userId) {
        // LuaScript 기반 원자적 처리
        return queueUtil.tryEnterActiveAtomic(eventId, userId, MAX_ACTIVE);
    }

    @Override
    // 예약 팝업창에서 퇴장
    public void leaveActive(String eventId, String userId) {
        queueUtil.leaveActive(eventId, userId);
    }

    @Override
    // 현재 예약 진행중인 사용자 수 조회
    public Long getActiveCount(String eventId) {
        return queueUtil.getActiveCount(eventId);
    }

    @Override
    // 사용자가 예약 팝업창에 입장 가능한지 확인
    public boolean canEnterReservation(String eventId, String userId) {
        Long rank = queueUtil.getUserRank(eventId, userId);
        if (rank == null) return false;

        long currentActive = queueUtil.getActiveCount(eventId);
        return rank == 1 && currentActive < MAX_ACTIVE;
    }

    @Override
    // 이벤트 대기열 크기 조회
    public Long getQueueCount(String eventId) {
        return queueUtil.getQueueCount(eventId);
    }

    @Override
    // heartbeat 갱신
    public void updateLastActive(String eventId, String userId) {
        queueUtil.updateHeartbeat(eventId, userId);
    }

    // 주기적 inactive 사용자 제거 (스케줄러에서 호출)
    public void removeInactiveUsersScheduled() {
        Set<String> queueKeys = queueUtil.getAllQueueKeys();
        long threshold = System.currentTimeMillis() - 10_000; // 10초
        for (String queueKey : queueKeys) {
            String eventId = queueUtil.extractEventId(queueKey);
            queueUtil.removeInactiveUsersAtomic(eventId, threshold);
        }
    }

}
