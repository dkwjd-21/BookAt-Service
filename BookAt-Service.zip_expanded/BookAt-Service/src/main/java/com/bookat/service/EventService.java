package com.bookat.service;

import java.util.List;

import com.bookat.dto.EventResDto;

public interface EventService {
	public List<EventResDto> selectAll();
	public EventResDto selectOne(int event_id);
	public List<EventResDto> selectByLocalCode(String local_code);
	public List<EventResDto> selectByLocalCodeAndStartTime(String local_code);
	public List<EventResDto> selectByLocalCodeAndCloseTime(String local_code);
	public List<EventResDto> selectForMain();
	public List<EventResDto> selectByStartTime();
	public List<EventResDto> selectByCloseTime();
	
	public int insert(EventResDto dto);	//아직 사용 안함
	public int udpate(EventResDto dto);
	public int delete(int event_id);
}
