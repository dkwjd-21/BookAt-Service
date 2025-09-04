package com.bookat.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResDto {
	
	private int event_id;
	private int book_id;
	private String ticket_type;
	private String event_name;
	private String event_description;
	private int event_price;
	private String event_image;
	private String local_code;
	private String address;
	private Date event_date;

}
