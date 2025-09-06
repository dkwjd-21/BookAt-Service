package com.bookat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
	
	@GetMapping("api/pay/test")
	public String test() {
		return "OK!";
	}

}
