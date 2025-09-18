package com.bookat.service;

import java.util.List;

import com.bookat.dto.EventSeatInfoDto;

public interface SeatService {
	List<EventSeatInfoDto> getSeatList(int eventId, int scheduleId);
}
