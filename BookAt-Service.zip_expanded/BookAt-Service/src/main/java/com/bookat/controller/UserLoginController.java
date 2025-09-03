package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.entity.UserLogin;
import com.bookat.service.impl.UserLoginServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLoginController {
	
	private final UserLoginServiceImpl loginService;

	@GetMapping("/login")
	public String loginForm(Model model) {
		
		model.addAttribute("userLogin", new UserLogin());
		
		return "user/loginForm";
	}
	
	@PostMapping("/login")
	public String login(@ModelAttribute UserLogin userLogin) {
		
		UserLogin user = loginService.login(userLogin);
		
		log.info("userId : {}", user.getUserId());
		log.info("userPw : {}", user.getUserPw());
		log.info("userName : {}", user.getUserName());
		
		return "redirect:/";
	}
}
