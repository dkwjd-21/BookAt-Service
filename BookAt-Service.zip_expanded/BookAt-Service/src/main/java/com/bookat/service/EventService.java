package com.bookat.service;

import java.util.List;

import com.bookat.dto.EventResDto;

public interface EventService {
	public List<EventResDto> selectAll();
	public EventResDto selectOne(int event_id);
	public List<EventResDto> selectByLocalCode(String local_code);
	
	public int insert(EventResDto dto);
	public int udpate(EventResDto dto);
	public int delete(int event_id);
}
