package com.bookat.service.impl;

import org.springframework.stereotype.Service;

import com.bookat.entity.Event;
import com.bookat.mapper.ReservationMapper;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
	
	private final ReservationMapper reservationMapper;

	@Override
	public Event startReservation(int eventId) {
		return reservationMapper.findEventByEventId(eventId);
	}

}
