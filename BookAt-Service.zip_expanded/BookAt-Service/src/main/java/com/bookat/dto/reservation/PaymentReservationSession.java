package com.bookat.dto.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bookat.domain.PaymentStatus;

public record PaymentReservationSession (
		
		String reservationToken,	// 해당 예약 세션 토큰
		int eventId,				// 이벤트 정보
		int scheduleId,				// 이벤트 회차 정보
		int reservedCount,			// 예약한 인원 수
		String method,				// 결제 수단
		BigDecimal amount,			// 총 결제 금액
		String merchantUid,			// 가맹점 주문번호
		String impUid,				// 포트원 결제번호
		String userId,
	    PaymentStatus status,		// 결제 상태
	    LocalDateTime createdAt
		
) {}
