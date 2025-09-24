package com.bookat.service;

import java.util.List;

import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.SeatTypeReqDto;
import com.bookat.dto.reservation.UserInfoReqDto;
import com.bookat.entity.reservation.EventPart;

public interface ReservationService {

	ReservationStartDto startReservation(int eventId, String userId);
	void selectSchedule(String reservationToken, int scheduleId);
	void selectPersonType(String reservationToken, PersonTypeReqDto personTypeReqDto);
	boolean inputUserInfo(String reservationToken, String userId, UserInfoReqDto userInfoReqDto);
	void cancelReservation(String reservationToken);
	void validateReservation(String reservationToken);
	
	// eventId로 회차 리스트 조회 
	List<EventPart> selectPartsByEventId(int eventId);	
	
	void selectSeatType(String reservationToken, SeatTypeReqDto reqDto);
	void confirmBooking(String reservationToken, SeatTypeReqDto reqDto);
}
