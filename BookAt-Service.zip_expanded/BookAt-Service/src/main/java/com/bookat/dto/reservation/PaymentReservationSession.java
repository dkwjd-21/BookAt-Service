package com.bookat.dto.reservation;

import java.math.BigDecimal;

public record PaymentReservationSession (
		
		String reservationToken,
		int eventId,
		int scheduleId,
		String method,
		BigDecimal amount,
		String merchantUid,
		String userId,
	    String status,
	    String createdAt
		
) {}
