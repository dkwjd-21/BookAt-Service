package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;


@Controller
public class TestController {
	
	@GetMapping("/")
	public String home(Model model, Authentication auth) {
		
	    boolean isLoggedIn = (auth != null && auth.isAuthenticated());
	    model.addAttribute("isLoggedIn", isLoggedIn);
	    
		return "home";
	}
	
//	@GetMapping("/pay/test")
//	public String test() {
//		return "OK!";
//	}

}
