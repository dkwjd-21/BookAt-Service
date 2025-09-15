package com.bookat.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 	CREATE TABLE EVENT (
	    EVENT_ID          NUMBER          PRIMARY KEY,
	    BOOK_ID           VARCHAR2(20)    NULL,
	    TICKET_TYPE       VARCHAR2(20)    NOT NULL,
	    EVENT_NAME        VARCHAR2(200)   NOT NULL,
	    EVENT_DESCRIPTION  VARCHAR2(4000)  NOT NULL,
	    EVENT_PRICE       NUMBER          NOT NULL,
	    EVENT_IMAGE       VARCHAR2(1000)  NULL,
	    LOCAL_CODE        VARCHAR2(20)    NOT NULL,
	    ADDRESS           VARCHAR2(2000)  NOT NULL
	);
 * */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
	
	private int eventId;
	private String bookId;
	private String ticketType;
	private String eventName;
	private String eventDescription;
	private int eventPrice;
	private String eventImage;
	private String localCode;
	private String address;
	
}
