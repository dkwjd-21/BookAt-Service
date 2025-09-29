package com.bookat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatValidationRequestDto {
	private int eventId;
	private int scheduleId;
	private List<String> seatNames;
}
