package com.bookat.scheduler;

import java.util.Date;
import java.util.List;

import com.bookat.dto.EventResDto;
import com.bookat.service.impl.EventServiceImpl;

public class EventSchedulerTestMain {
    public static void main(String[] args) {
        System.out.println("EventSchedulerTestMain 실행");

        EventServiceImpl eventServiceImpl = new EventServiceImpl();
        
        Date today = new Date();
        
        List<EventResDto> list = eventServiceImpl.selectByEventDate(today);
        System.out.println(list);
    }
}

