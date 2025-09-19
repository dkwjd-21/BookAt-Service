package com.bookat.entity.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 이벤트 좌석

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatType {

	private int seatId;
	private String seatName;
	private int seatStatus;
	private String seatGradeType;
	private int eventId;
	private int scheduleId;
	
}
