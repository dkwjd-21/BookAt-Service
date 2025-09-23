package com.bookat.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

	private int paymentId;
	private int totalPrice;
	private int paymentPrice;
	private String paymentMethod;
	private int paymentStatus;
	private Date paymentDate;
	private String paymentInfo;
	
}
