package com.bookat.service;

import java.util.List;

import com.bookat.dto.myPage.ReservationDetailDto;
import com.bookat.dto.myPage.TicketDetailDto;

public interface MyPageService {
	
	public List<ReservationDetailDto> getReservationDetails(String userId);
	public List<TicketDetailDto> getTicketDetails(int reservationId);

}
