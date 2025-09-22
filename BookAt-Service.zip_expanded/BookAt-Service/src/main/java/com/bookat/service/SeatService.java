package com.bookat.service;

import java.util.List;

import com.bookat.dto.EventSeatInfoDto;

public interface SeatService {
	List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId);
	boolean checkAllSeatsAvailable(int eventId, int scheduleId, List<String> seatNames);
	boolean holdSeats(int eventId, int scheduleId, List<String> seatNames);
}
