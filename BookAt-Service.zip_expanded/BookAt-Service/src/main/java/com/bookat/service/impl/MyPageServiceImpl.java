package com.bookat.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bookat.domain.ReservationStatus;
import com.bookat.dto.myPage.ReservationDetailDto;
import com.bookat.dto.myPage.TicketDetailDto;
import com.bookat.mapper.MyPageMapper;
import com.bookat.service.MyPageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {
	
	private final MyPageMapper myPageMapper;

	@Override
	public List<ReservationDetailDto> getReservationDetails(String userId) {
		List<ReservationDetailDto> reservations = myPageMapper.getReservationDetails(userId);

		for(ReservationDetailDto reservation : reservations) {
			reservation.setReservationStatus(ReservationStatus.fromCode(Integer.parseInt(reservation.getReservationStatus())).toString());
		}
		
		return reservations;
	}

	@Override
	public List<TicketDetailDto> getTicketDetails(int reservationId) {
		return myPageMapper.getTicketDetails(reservationId);
	}

}
