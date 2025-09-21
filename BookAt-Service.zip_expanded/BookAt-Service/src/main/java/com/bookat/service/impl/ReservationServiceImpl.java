package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.mapper.ReservationMapper;
import com.bookat.service.ReservationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
	
	@Autowired
	private final ReservationMapper reservationMapper;

	@Override
	public Event startReservation(int eventId) {
		return reservationMapper.findEventByEventId(eventId);
	}

	@Override
	public List<EventPart> selectPartsByEventId(int eventId) {
		return reservationMapper.findPartsByEventId(eventId);
	}

}
