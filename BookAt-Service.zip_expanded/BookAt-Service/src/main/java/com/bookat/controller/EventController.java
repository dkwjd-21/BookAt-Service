package com.bookat.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bookat.dto.EventResDto;
import com.bookat.entity.Book;
import com.bookat.entity.Review;
import com.bookat.service.impl.EventServiceImpl;
import com.bookat.service.impl.ReviewServiceImpl;

@Controller
@RequestMapping("/events")
public class EventController {
	
	@Autowired
	private EventServiceImpl eventService;
	
	@Autowired
	private ReviewServiceImpl reviewService;
	
	// application.properties에서 키 값을 주입받습니다.
    @Value("${kakao.maps.appkey}")
    private String kakaoMapsAppkey;
	
	
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
        model.addAttribute("book", book);
        
        List<Review> reviews = reviewService.findByEventId(event.getEventId());
        model.addAttribute("reviews",reviews);
        System.out.println("불러온 리뷰 : "+reviews);
        
        int reviewCount = reviewService.countByEventId(event.getEventId());
        model.addAttribute("reviewCount",reviewCount);
        
        model.addAttribute("kakaoMapsAppkey", kakaoMapsAppkey);

        // 상세 페이지 뷰를 반환 
        return "mainpage/event_detail";
    }
	
	
	@GetMapping("/{eventId}/reservation")
	public String reservationPage(@PathVariable int eventId, Model model, RedirectAttributes redirectAttributes) {

	    EventResDto event = eventService.selectOne(eventId);

	    // 이벤트가 존재하지 않을 경우의 예외 처리
	    if (event == null) {
	        redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 이벤트입니다.");
	        return "redirect:/event";
	    }

	    // [권장] DTO의 eventDate 필드를 Date -> LocalDateTime으로 변경하면 아래 변환 과정이 필요 없어집니다.
	    // 1. [수정] 타임존을 서버 기본값 대신 'Asia/Seoul'로 명시
	    ZoneId zoneId = ZoneId.of("Asia/Seoul");
	    LocalDateTime eventDateTime = event.getEventDate().toInstant()
	                                       .atZone(zoneId)
	                                       .toLocalDateTime();
	    
	    LocalDateTime now = LocalDateTime.now(zoneId);

	    // 2. [수정] 예매 시작 및 마감 시간 계산 (프론트 JS 로직과 동기화)
	    // 예매 시작: 이벤트 날짜 30일 전 18:00
	    LocalDateTime ticketingOpenTime = eventDateTime.toLocalDate().minusDays(30).atTime(18, 0);
	    // 예매 마감: 이벤트 당일 00:00 (이벤트 당일이 되면 마감)
	    LocalDateTime ticketingCloseTime = eventDateTime.toLocalDate().atStartOfDay();

	    // 3. [수정] 현재 시간이 예매 기간을 벗어났는지 검증 (시작 전 OR 마감 후)
	    if (now.isBefore(ticketingOpenTime) || now.isAfter(ticketingCloseTime)) {
	        redirectAttributes.addFlashAttribute("errorMessage", "예매 가능한 시간이 아닙니다.");
	        return "redirect:/event/detail?event_id="+eventId;
	    }

	    // 예매 가능 시간일 경우, 정상적으로 예매 페이지로 이동
	    model.addAttribute("event", event);
	    return "reservation/reservation_page"; // 템플릿 경로를 명확하게 지정
	}
	
	
}
