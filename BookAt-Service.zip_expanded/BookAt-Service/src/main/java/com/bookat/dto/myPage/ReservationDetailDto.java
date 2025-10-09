package com.bookat.dto.myPage;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReservationDetailDto {

	private String eventName;				// event
	private String eventImg;				// event
	private Date scheduleTime;				// eventPart
	private String scheduleName;			// eventPart
	private String reservedCount;			// reservation
	private String reservationStatus;		// reservation
	private Date reservationDate;			// reservation
	private String totalPrice;				// payment
	
}
