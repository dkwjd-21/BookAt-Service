package com.bookat.service;

import com.bookat.dto.reservation.PaymentInfoResDto;
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
	PaymentInfoResDto getPaymentInfo(String reservationToken);
	int createReservationAndTicket(String paymentToken, String reservationToken);
	
}
