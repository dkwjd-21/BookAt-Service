package com.bookat.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResDto {
	

	private int eventId;
    private int bookId;
    private String ticketType;
    private String eventName;
    private String eventDescription;
    private int eventPrice;
    private String eventImage;
    private String localCode;
    private String address;
    private Date eventDate;


}
