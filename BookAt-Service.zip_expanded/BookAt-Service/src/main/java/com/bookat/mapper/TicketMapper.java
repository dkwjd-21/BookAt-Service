package com.bookat.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.reservation.Ticket;

@Mapper
public interface TicketMapper {

	int insertTicket(Ticket ticket);
	List<Map<String, Object>> findTicketsByReservationId(String reservationId);
	
}
