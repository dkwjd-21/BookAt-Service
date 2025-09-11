package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.service.UserLoginService;
import com.bookat.util.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController{
	
    private final JwtTokenProvider jwt;
    private final UserLoginService userService;
    
    
    
    private String resolveToken(HttpServletRequest req) {
    	return null;
    }
    private String requiredUserId(HttpServletRequest req) {
    	return null;
    }
    
    @GetMapping("/checkout")
    public String checkout() {
    	 return "payment/checkout";
    	
    }
    
    
    @PostMapping("/complete")
    public String complete() {
    	return null;
    }
}