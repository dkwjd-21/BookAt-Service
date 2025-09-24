package com.bookat.service;

import java.util.List;

import com.bookat.dto.EventSeatDto;
import com.bookat.dto.EventSeatInfoDto;

public interface SeatService {
	List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId);

	boolean checkAllSeatsAvailable(int eventId, int scheduleId, List<String> seatNames);

	boolean holdSeats(int eventId, int scheduleId, List<String> seatNames);

	// 좌석 insert
	int insertSeat(EventSeatDto dto);
	
	EventSeatDto selectOneBySeatName(String seatName, int eventId, int scheduleId);
	
	int updateSeatStatus(EventSeatDto dto);
	
	// 좌석 상태 : 홀드/예매완료된 좌석 해제 
	boolean releaseSeats(int eventId, int scheduleId, List<String> seatNames);
	
	// 좌석 상태 : 예매완료 처리 
	
}
