package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.reservation.Ticket;

@Mapper
public interface TicketMapper {

	int insertTicket(Ticket ticket);
	
}
