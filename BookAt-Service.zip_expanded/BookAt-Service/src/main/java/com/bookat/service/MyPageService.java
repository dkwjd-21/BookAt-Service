package com.bookat.service;

import java.util.List;

import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;

public interface MyPageService {
	
	public List<Reservation> getReservations(String userId);
	public List<Ticket> getTickets(int reservationId);

}
