package com.bookat.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bookat.entity.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class HomeController {
	
	@GetMapping("/")
	public String home() {
	    
		return "home";
	}
	
	@GetMapping("/mainPage")
	public String mainPage(@AuthenticationPrincipal User user, Model model) {
		if(user != null) {
			log.info("mainPage user : {}", user.getUserId());
			
			model.addAttribute("userId", user.getUserId());
			model.addAttribute("userName", user.getUserName());
		}
		
		return "home";
	}

}
