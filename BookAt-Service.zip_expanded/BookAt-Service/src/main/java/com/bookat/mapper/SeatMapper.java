package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.dto.EventSeatDto;

@Mapper
public interface SeatMapper {
	// 회차별 좌석 insert
	int insertSeat(EventSeatDto dto);
	EventSeatDto selectOneBySeatName(String seatName, int eventId, int scheduleId);
	int updateSeatStatus(EventSeatDto dto);
}
