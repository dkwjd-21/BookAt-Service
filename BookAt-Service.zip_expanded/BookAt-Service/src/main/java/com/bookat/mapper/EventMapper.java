package com.bookat.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookat.dto.EventPartDto;
import com.bookat.dto.EventResDto;
import com.bookat.dto.EventSeatDto;
import com.bookat.entity.Book;

@Mapper
public interface EventMapper {
	

	//@Select(" SELECT * FROM EVENT ORDER BY EVENT_ID ASC ")
	List<EventResDto> selectEventList();
	 
	//@Select(" SELECT * FROM EVENT WHERE EVENT_ID =#{event_id}")
	EventResDto selectEventOne(int event_id);
	
	//@Select( " SELECT * FROM EVENT WHERE LOCAL_CODE=#{local_code} ")
	List<EventResDto> selectByLocalCode(String local_code);
	
	//@Select( " SELECT * FROM EVENT WHERE LOCAL_CODE=#{local_code} AND EVENT_DATE > SYSDATE + 30 ORDER BY EVENT_DATE ASC ")
	List<EventResDto> selectByLocalCodeAndStartTime(String local_code);
	
	//Select( " SELECT * FROM EVENT WHERE LOCAL_CODE=#{local_code} AND EVENT_DATE > SYSDATE ORDER BY EVENT_DATE ASC ")

	List<EventResDto> selectByLocalCodeAndCloseTime(String local_code);
	

	List<EventResDto> selectForMain();
	
	List<EventResDto> selectByStartTime();

	List<EventResDto> selectByCloseTime();
	
	Book selectBookOne(String book_id);

	List<EventResDto> selectByBookId(@Param("bookId") String bookId);
	
 	List<EventResDto> selectByEventDate(Date eventDate);

	// 이벤트 아이디로 회차 조회
 	List<EventPartDto> selectPartByEventId(int eventId);
 	
 	// 회차별 좌석 insert
 	int insertSeat(EventSeatDto dto);
}
