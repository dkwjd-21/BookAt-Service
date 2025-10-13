package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.dto.myPage.ReservationDetailDto;
import com.bookat.dto.myPage.TicketDetailDto;

@Mapper
public interface MyPageMapper {

	List<ReservationDetailDto> getReservationDetails(String userId);
	List<TicketDetailDto> getTicketDetails(int reservationId);
	
}
