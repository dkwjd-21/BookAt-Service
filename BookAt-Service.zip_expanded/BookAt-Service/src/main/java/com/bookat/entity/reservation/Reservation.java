package com.bookat.entity.reservation;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 이벤트 예매

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

	private int reservationId;
	private int paymentId;
	private Date reservationDate;
	private int reservationStatus;
	private int scheduleId;
	private String userId;
	
}
