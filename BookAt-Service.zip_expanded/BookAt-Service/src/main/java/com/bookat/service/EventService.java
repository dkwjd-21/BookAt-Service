package com.bookat.service;

import java.util.Date;
import java.util.List;

import com.bookat.dto.BookDto;
import com.bookat.dto.EventResDto;
import com.bookat.entity.Book;

public interface EventService {
	public List<EventResDto> selectAll();
	public EventResDto selectOne(int event_id);
	public List<EventResDto> selectByLocalCode(String local_code);
	public List<EventResDto> selectByLocalCodeAndStartTime(String local_code);
	public List<EventResDto> selectByLocalCodeAndCloseTime(String local_code);
	public List<EventResDto> selectForMain();
	public List<EventResDto> selectByStartTime();
	public List<EventResDto> selectByCloseTime();
	public Book selectBookOne(String book_id);
	public List<EventResDto> selectByEventDate(Date eventDate);
	
	public int insert(EventResDto dto);	//아직 사용 안함
	public int udpate(EventResDto dto);
	public int delete(int event_id);
	
	public List<EventResDto> selectByBookId(String bookId);
}
