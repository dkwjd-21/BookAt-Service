package com.bookat.dto.reservation;

import lombok.Data;

@Data
public class PaymentInfoResDto {

	private int eventId;
	private int scheduleId;
	private String title;
	private int totalPrice;
	private int reservedCount;
	
}
