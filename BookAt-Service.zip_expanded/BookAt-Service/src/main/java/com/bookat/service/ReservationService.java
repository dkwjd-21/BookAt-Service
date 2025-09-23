package com.bookat.service;

import java.util.List;

import com.bookat.entity.Event;
import com.bookat.entity.reservation.EventPart;
import com.bookat.dto.reservation.PersonTypeReqDto;
import com.bookat.dto.reservation.ReservationStartDto;
import com.bookat.dto.reservation.UserInfoReqDto;

public interface ReservationService {

	ReservationStartDto startReservation(int eventId, String userId);
	void selectSchedule(String reservationToken, int scheduleId);
	void selectPersonType(String reservationToken, PersonTypeReqDto personTypeReqDto);
	boolean inputUserInfo(String reservationToken, String userId, UserInfoReqDto userInfoReqDto);
	void cancelReservation(String reservationToken);
	void validateReservation(String reservationToken);
	
	// eventId로 회차 리스트 조회 
	List<EventPart> selectPartsByEventId(int eventId);	
}
