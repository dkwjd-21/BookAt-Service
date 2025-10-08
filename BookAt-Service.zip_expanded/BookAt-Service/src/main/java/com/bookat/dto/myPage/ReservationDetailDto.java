package com.bookat.dto.myPage;

import com.bookat.entity.Payment;
import com.bookat.entity.reservation.EventPart;
import com.bookat.entity.reservation.Reservation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservationDetailDto {

	/*
	 * 이벤트 제목
	 * 실시 날짜와 회차 (시간)
	 * 총 인원수
	 * 총 가격
	 * 결제완료 상태
	 * */
	private Reservation reservation;
	private String eventName;
	private EventPart eventPart;
	private Payment payment;
	
}
