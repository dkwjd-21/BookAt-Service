package com.bookat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.dto.EventResDto;
import com.bookat.service.impl.EventServiceImpl;

@Controller
@RequestMapping("/event")
public class EventController {
	
	@Autowired
	private EventServiceImpl eventService;
	
	
	@GetMapping("/selectAll")	//전체 리스트 불러오기
	public List<EventResDto> selectAll(Model model){
		List<EventResDto> eventList = eventService.selectAll();
		
		model.addAttribute("eventList",eventList);
		

		return eventList; 


	}
	
	@GetMapping("/selectOne")	//이벤트 아이디로 이벤트 하나 불러오기
	public EventResDto selectOne(int event_id, Model model) {
		
		EventResDto event = eventService.selectOne(event_id);
		model.addAttribute("event",event);
		
		return event;	
	}
	

	@GetMapping	//메인 페이지로 이동한다.
	public String eventMainpage(Model model) { 

	    List<EventResDto> res = eventService.selectForMain();
	    model.addAttribute("event",res);
	    
	    List<EventResDto> resOpenTime = eventService.selectByStartTime(); 
	    model.addAttribute("openTime",resOpenTime);

	    List<EventResDto> resCloseTime = eventService.selectByCloseTime();
	    model.addAttribute("closeTime",resCloseTime);

	    // 현재 선택된 지역 코드를 모델에 추가 

	    return "mainpage/event_mainpage";	//바꿀부분
	}
	
	@GetMapping("/category")	//카테고리별 페이지로 이동한다. 
	public String eventMainpage(@RequestParam(name = "local_code", required = false, defaultValue = "SEOUL") String local_code, Model model) {

	    List<EventResDto> res = eventService.selectByLocalCode(local_code);
	    model.addAttribute("event",res);

	    List<EventResDto> resOpenTime = eventService.selectByLocalCodeAndStartTime(local_code); 
	    model.addAttribute("openTime",resOpenTime);

	    List<EventResDto> resCloseTime = eventService.selectByLocalCodeAndCloseTime(local_code);
	    model.addAttribute("closeTime",resCloseTime);


	    // 현재 선택된 지역 코드를 모델에 추가 

	    model.addAttribute("currentLocalCode", local_code);

	    return "mainpage/event_categorypage";
	};
	
	@GetMapping("/detail")
    public String eventDetail(@RequestParam("event_id") int event_id, Model model){
        
        // selectOne 메소드를 사용하여 특정 이벤트 데이터를 조회
        EventResDto event = eventService.selectOne(event_id);
        
        // 조회된 데이터를 "event"라는 이름으로 모델에 추가
        model.addAttribute("event", event);
        
        // 상세 페이지 뷰를 반환 
        return "mainpage/event_detail";
    }
	
	
	
}
