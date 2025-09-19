package com.bookat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventSeatInfoDto {
	// 프론트 UI 렌더링을 위한 좌석 데이터 (from Redis) 
	private String seatName;
	private String status;		// AVAILABLE, HOLD, BOOKED
}
