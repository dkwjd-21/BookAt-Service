package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.bookat.dto.EventResDto;

@Mapper
public interface EventMapper {
	
	@Select(" SELECT * FROM EVENT ORDER BY EVENT_ID ASC ")
	List<EventResDto> selectEventList();
	
	@Select(" SELECT * FROM EVENT WHERE EVENT_ID =#{event_id}")
	EventResDto selectEventOne(int event_id);
	
	@Select( " SELECT * FROM EVENT WHERE LOCAL_CODE=#{local_code} ")
	List<EventResDto> selectByLocalCode(String local_code);
	
}
