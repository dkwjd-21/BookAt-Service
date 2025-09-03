package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

	@GetMapping("/")
	public String home() {
		return "home";
	}
	
	@GetMapping("/api/books/private")
	public String books() {
		return "/api/books private";
	}
	
}
