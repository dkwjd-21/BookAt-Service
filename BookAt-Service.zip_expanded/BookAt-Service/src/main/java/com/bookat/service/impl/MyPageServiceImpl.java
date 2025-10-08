package com.bookat.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;
import com.bookat.mapper.MyPageMapper;
import com.bookat.service.MyPageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {
	
	private final MyPageMapper myPageMapper;

	@Override
	public List<Reservation> getReservations(String userId) {
		
		return myPageMapper.getReservations(userId);
	}

	@Override
	public List<Ticket> getTickets(int reservationId) {
		
		return myPageMapper.getTickets(reservationId);
	}

}
