package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.reservation.EventPart;

@Mapper
public interface EventPartMapper {

	List<EventPart> findEventPartsByEventId(int eventId);
	
}
