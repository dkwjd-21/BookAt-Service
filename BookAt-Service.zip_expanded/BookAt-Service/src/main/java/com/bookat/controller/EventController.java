package com.bookat.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bookat.dto.BookDto;
import com.bookat.dto.EventResDto;
import com.bookat.entity.Book;
import com.bookat.entity.Event;
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
	
	@GetMapping("/detail")	//상세페이지로 이동
    public String eventDetail(@RequestParam("event_id") int event_id, Model model){
        
        EventResDto event = eventService.selectOne(event_id);
        model.addAttribute("event", event);
        
        Book book = eventService.selectBookOne(event.getBookId());
        System.out.println("res " + book);
        model.addAttribute("book", book);

        // 상세 페이지 뷰를 반환 
        return "mainpage/event_detail";
    }
	
	
	@GetMapping("/events/{eventId}/reservation")	//만약 사용자가 티켓팅url로 바로 접속하게 되면 현재 시간과 비교해서 접속을 차단한다.
	public String reservationPage(@PathVariable int eventId, Model model, RedirectAttributes redirectAttributes) {

	    EventResDto event = eventService.selectOne(eventId);

	    // 📌 [추가] 이벤트가 존재하지 않을 경우의 예외 처리
	    if (event == null) {
	        redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 이벤트입니다.");
	        return "redirect:/event/main"; // 또는 적절한 오류 페이지로 이동
	    }

	    // 1. 현재 서버 시간
	    LocalDateTime now = LocalDateTime.now();

	    // 2. 📌 [수정] java.util.Date를 java.time.LocalDateTime으로 변환
	    Date eventDateFromDb = event.getEventDate();
	    LocalDateTime eventDateTime = eventDateFromDb.toInstant()
	                                                 .atZone(ZoneId.systemDefault())
	                                                 .toLocalDateTime();

	    // 3. 📌 [수정] 예매 마감 시간을 계산: 이벤트 날짜의 하루 전 23:59:59
	    LocalDateTime ticketingCloseTime = eventDateTime.toLocalDate().minusDays(1).atTime(LocalTime.MAX);

	    // 4. 📌 [수정] 현재 시간이 예매 마감 시간을 지났는지 검증
	    if (now.isAfter(ticketingCloseTime)) {
	        // 예매 기간이 아닐 경우, 상세 페이지로 리다이렉트하며 에러 메시지를 전달.
	        redirectAttributes.addFlashAttribute("errorMessage", "예매 가능한 시간이 아닙니다.");
	        // URL을 @RequestParam 형식에 맞게 변경
	        return "redirect:/event/detail?event_id=" + eventId;
	    }

	    // 예매 가능 시간일 경우, 정상적으로 예매 페이지로 이동
	    model.addAttribute("event", event);
	    return "mainpage/reservation_page" ; // 템플릿 경로 추가해야함
	}
	
	
}
