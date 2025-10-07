package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.reservation.Reservation;
import com.bookat.entity.reservation.Ticket;

@Mapper
public interface MyPageMapper {

	List<Reservation> getReservations(String userId);
	List<Ticket> getTickets(int reservationId);
	
}
