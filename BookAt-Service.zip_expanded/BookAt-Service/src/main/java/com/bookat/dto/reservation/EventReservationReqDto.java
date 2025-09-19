package com.bookat.dto.reservation;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.entity.reservation.SeatType;
import com.bookat.entity.reservation.Ticket;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventReservationReqDto {

	private Event event;
	private EventPart eventPart;
	private Ticket ticket;
	private SeatType seatType;
	
}
