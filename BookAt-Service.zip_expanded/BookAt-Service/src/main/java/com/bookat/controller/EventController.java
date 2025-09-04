package com.bookat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.dto.EventResDto;
import com.bookat.service.impl.EventServiceImpl;

@Controller
@RequestMapping("/event")
public class EventController {
	
	@Autowired
	private EventServiceImpl eventService;
	
	
	@GetMapping("/selectAll")	//전체 리스트 불러오기
	public String selectAll(Model model){
		List<EventResDto> eventList = eventService.selectAll();
		
		model.addAttribute("eventList",eventList);
		
		return "event_mainpage"; 

	}
	
	@GetMapping("/selectOne")	//이벤트 아이디로 이벤트 하나 불러오기
	public String selectOne(int event_id, Model model) {
		
		EventResDto event = eventService.selectOne(event_id);
		model.addAttribute("event",event);
		
		return "";	//상세페이지 url 넣으면 될 것 같습니다. 
	}
	
	@GetMapping("/selectByLocalCode")	//지역코드 넣어서 가져오기
	public String selectByLocalCode(String local_code, Model model){
		
		List<EventResDto> res = eventService.selectByLocalCode("SEOUL");
		System.out.println(res);
		
		model.addAttribute("event",res);
		
		return "mainpage/event_mainpage";
	}
	
	
	
	
}
