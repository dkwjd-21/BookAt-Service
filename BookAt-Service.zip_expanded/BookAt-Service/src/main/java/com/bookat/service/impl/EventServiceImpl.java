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
	public List<EventResDto> selectAll() {	//전체 선택하기
		
		return mapper.selectEventList();
	}

	@Override
	public EventResDto selectOne(int event_id) {	//하나만 선택하기

		return mapper.selectEventOne(event_id);
	}
	
	@Override
	public List<EventResDto> selectByLocalCode(String local_code) {	//지역코드로 선택하기
		
		return mapper.selectByLocalCode(local_code);
	}
	
	@Override
	public List<EventResDto> selectByLocalCodeAndStartTime(String local_code) {	//지역코드로 선택 + 티켓팅 시작시간 기준 빠른순
		
		return mapper.selectByLocalCodeAndStartTime(local_code);
	}
	
	@Override
	public List<EventResDto> selectByLocalCodeAndCloseTime(String local_code) {	//지역코드 선택 + 티켓팅 마감시간 기준 빠른순
		
		return mapper.selectByLocalCodeAndCloseTime(local_code);
	}
	
	@Override
	public List<EventResDto> selectForMain() {
		
		return mapper.selectForMain();
	}
	
	@Override
	public List<EventResDto> selectByStartTime() {
		
		return mapper.selectByStartTime();
	}

	@Override
	public List<EventResDto> selectByCloseTime() {
		
		return mapper.selectByCloseTime();
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
