package com.bookat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventPartDto {
	private int scheduleId;
	private LocalDateTime scheduleTime;
	private String scheduleName;
	private int eventId;
	private int remainingSeat;
}
