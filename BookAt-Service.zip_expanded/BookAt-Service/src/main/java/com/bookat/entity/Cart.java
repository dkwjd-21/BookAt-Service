package com.bookat.entity;

import java.util.Date;

import lombok.Data;

@Data
public class Cart {
	private String cartId;
	private String userId;
	private String bookId;
	private int cartQuantity;
	private Date cartGegdate;
}
