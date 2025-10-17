package com.bookat.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
	 private String cartId;
	    private int cartQuantity;
	    private String bookId;
	    private String title;
	    private String author;
	    private int price;
	    private String coverImage;
}
