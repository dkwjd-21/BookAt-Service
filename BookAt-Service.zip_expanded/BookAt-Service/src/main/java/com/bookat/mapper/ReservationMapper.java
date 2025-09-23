package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;

@Mapper
public interface ReservationMapper {
	
	Event findEventByEventId(int eventId);
	int insertReservation(Reservation reservation);
	int insertTicket(Ticket ticket);

}
