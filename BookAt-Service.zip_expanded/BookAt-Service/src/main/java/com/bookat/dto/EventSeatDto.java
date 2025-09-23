package com.bookat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSeatDto {
	private int seatId;
	private String seatName;
	private int seatStatus;			// 1:예매가능, -1:홀드, 0:예매됨
	private String seatGradeType;
	private int eventId;
	private int scheduleId;
}
