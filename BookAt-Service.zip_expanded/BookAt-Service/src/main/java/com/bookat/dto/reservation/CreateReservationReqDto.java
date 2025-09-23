package com.bookat.dto.reservation;

import lombok.Data;

@Data
public class CreateReservationReqDto {

	private int paymentId;
	private int scheduleId;
	private String userId;
	
	private int adultCount;
	private int youthCount;
	private int childCount;
	
	private String userName;
	private String phone;
	private String email;
	
}
