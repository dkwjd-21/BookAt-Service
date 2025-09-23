package com.bookat.entity.reservation;

import java.util.Date;

import com.bookat.enums.PersonType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 티켓

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
	
	private int ticketId;
	private Date ticketCreatedDate;
	private int ticketStatus;
	private String ticketType;		// 'PERSON_TYPE' , 'SEAT_TYPE'
	private PersonType personType;		// 'ADULT' , 'YOUTH' , 'CHILD' (인원형일 때)
	private int reservationId;
	private Integer seatId;
	private int paymentId;
	
}
