package com.bookat.dto.reservation;

import java.util.List;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStartDto {

	private Event event;
	private List<EventPart> eventParts;
	private String reservationToken;
	
}
