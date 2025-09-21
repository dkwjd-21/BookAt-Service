package com.bookat.service;

import java.util.List;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;

public interface ReservationService {

	Event startReservation(int eventId);
	
	// eventId로 회차 리스트 조회 
	List<EventPart> selectPartsByEventId(int eventId);	
}
