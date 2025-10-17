package com.bookat.service;

import java.util.List;

import com.bookat.dto.EventSeatDto;
import com.bookat.dto.EventSeatInfoDto;

public interface SeatService {
	List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId);
	boolean checkAllSeatsAvailable(int eventId, int scheduleId, List<String> seatNames);
	boolean holdSeats(int eventId, int scheduleId, List<String> seatNames);
	boolean releaseSeats(int eventId, int scheduleId, List<String> seatNames);
	boolean confirmSeats(int eventId, int scheduleId, List<String> seatNames);
	int insertSeat(EventSeatDto dto);
	EventSeatDto selectOneBySeatName(String seatName, int eventId, int scheduleId);
	int updateSeatStatus(EventSeatDto dto);
}
