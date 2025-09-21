package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;

@Mapper
public interface ReservationMapper {
	
	Event findEventByEventId(int eventId);

	List<EventPart> findPartsByEventId(int eventId);
}
