package com.bookat.service;

import java.util.List;
import java.util.Map;

import com.bookat.dto.reservation.PaymentInfoResDto;
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
	void cancelReservation(String reservationToken, boolean isPaymentStep, String reason);
	void validateReservation(String reservationToken);
	PaymentInfoResDto getPaymentInfo(String reservationToken);
	void createReservation(String reservationToken, Long paymentId);
	// 예매 완료
	void completeReservation(String reservationToken, String userId);
	
	// eventId로 회차 리스트 조회 
	List<EventPart> selectPartsByEventId(int eventId);	
	
	void selectSeatType(String reservationToken, SeatTypeReqDto reqDto);
	void confirmBooking(String reservationToken, SeatTypeReqDto reqDto);
	
	// 기존 예매 번호로 예약 변경에 필요한 티켓 및 좌석 정보 조회 
	List<Map<String, Object>> getExistingReservationData(String reservationId);
	
	// 예매 변경
	public void modifySeats(String reservationToken, int eventId, int scheduleId,
            List<String> beforeSeats, List<String> afterSeats);
}
