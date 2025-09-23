package com.bookat.entity.reservation;

import java.util.Date;

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
	private String ticketType;
	private String personType;
	private int reservationId;
	private int seatId;
	private int paymentId;
	
}
