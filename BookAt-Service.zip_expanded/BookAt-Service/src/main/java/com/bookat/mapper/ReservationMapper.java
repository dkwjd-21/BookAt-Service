package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.entity.reservation.Reservation;

@Mapper
public interface ReservationMapper {
	
	Event findEventByEventId(int eventId);
	int insertReservation(Reservation reservation);

	List<EventPart> findPartsByEventId(int eventId);
	
	int updateReservationStatus(int reservationId, int status);
	Reservation selectReservationByReservaionId(int reservationId);
}
