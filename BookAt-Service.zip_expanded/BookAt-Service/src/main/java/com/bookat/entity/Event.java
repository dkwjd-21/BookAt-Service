package com.bookat.entity;

import java.util.Date;

// 이 클래스는 EVENT 테이블과 매핑되는 엔티티
public class Event {

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
    
    public Event() {}
    

    public Event(int eventId, int bookId, String ticketType, String eventName, String eventDescription,
                 int eventPrice, String eventImage, String localCode, String address, Date eventDate) {
        super();
        this.eventId = eventId;
        this.bookId = bookId;
        this.ticketType = ticketType;
        this.eventName = eventName;
        this.eventDescription = eventDescription;
        this.eventPrice = eventPrice;
        this.eventImage = eventImage;
        this.localCode = localCode;
        this.address = address;
        this.eventDate = eventDate;
    }
    

    public int getEventId() {
        return eventId;
    }
    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
    public int getBookId() {
        return bookId;
    }
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }
    public String getTicketType() {
        return ticketType;
    }
    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }
    public String getEventName() {
        return eventName;
    }
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    public String getEventDescription() {
        return eventDescription;
    }
    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }
    public int getEventPrice() {
        return eventPrice;
    }
    public void setEventPrice(int eventPrice) {
        this.eventPrice = eventPrice;
    }
    public String getEventImage() {
        return eventImage;
    }
    public void setEventImage(String eventImage) {
        this.eventImage = eventImage;
    }
    public String getLocalCode() {
        return localCode;
    }
    public void setLocalCode(String localCode) {
        this.localCode = localCode;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public Date getEventDate() {
        return eventDate;
    }
    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }
}