package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class HomeController {
	
    @GetMapping("/")
    public String root() {
        return "redirect:/books";
    }
	
	// 테스트를 위한 메인 페이지 
	@GetMapping("/infoPage/reservationTest")
	public String queue(Model model) {
		return "reservation/QueueModal";
	}
	
	// 테스트 페이지
	@GetMapping("/test/")
	public String test() {
		return "test";
	}
	
}
