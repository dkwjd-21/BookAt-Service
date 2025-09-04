package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookat.dto.EventResDto;
import com.bookat.mapper.EventMapper;
import com.bookat.service.EventService;

@Service
public class EventServiceImpl implements EventService{
	
	@Autowired
	private EventMapper mapper;

	@Override
	public List<EventResDto> selectAll() {
		
		return mapper.selectEventList();
	}

	@Override
	public EventResDto selectOne(int event_id) {

		return mapper.selectEventOne(event_id);
	}
	
	@Override
	public List<EventResDto> selectByLocalCode(String local_code) {
		
		return mapper.selectByLocalCode(local_code);
	}
	
	
	
	
	

	@Override
	public int insert(EventResDto dto) {

		return 0;	//미완
	}

	@Override
	public int udpate(EventResDto dto) {

		return 0;	//미완
	}

	@Override
	public int delete(int event_id) {

		return 0;	//미완
	}

	

}
