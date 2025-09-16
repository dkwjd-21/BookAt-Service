package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.Event;

@Mapper
public interface ReservationMapper {
	
	Event findEventByEventId(int eventId);

}