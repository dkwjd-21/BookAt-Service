package com.bookat.entity.reservation;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 이벤트 회차

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventPart {
	
	private int scheduleId;
	private Date scheduleTime;
	private String scheduleName;
	private int eventId;
	private int remainingSeat;

}
