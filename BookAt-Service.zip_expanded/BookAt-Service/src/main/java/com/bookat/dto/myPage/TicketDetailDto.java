package com.bookat.dto.myPage;

import java.util.Date;

import com.bookat.enums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TicketDetailDto {

	private String eventName;			// reservation (event_id) -> event
	private String scheduleName;		// reservation (schedule_id) -> event_part
	private Date scheduleTime;			// reservation (schedule_id) -> event_part
	private PersonType personType;		// ticket
	private String seatName;			// seat_type (seat_id)
	private String ticketType;			// ticket
	
}
