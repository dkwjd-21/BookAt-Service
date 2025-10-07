package com.bookat.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;
import com.bookat.service.MyPageService;

@Service
public class MyPageServiceImpl implements MyPageService {

	@Override
	public List<Reservation> getReservations(String userId) {
		
		return null;
	}

	@Override
	public List<Ticket> getTickets(int reservationId) {
		
		return null;
	}

}
