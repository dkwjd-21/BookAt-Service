package com.bookat.dto.reservation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatTypeReqDto {
	private int eventId;
	private int scheduleId;
	private List<String> seatNames;
	private int totalPrice;
}
