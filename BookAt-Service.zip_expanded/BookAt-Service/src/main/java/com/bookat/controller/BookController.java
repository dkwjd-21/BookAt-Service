package com.bookat.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bookat.service.BookService;

import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor

public class BookController{

	private final BookService bookService;
	
	// 메인
	@GetMapping("/books")
	public String booksHome(Model model) {
		model.addAttribute("best", bookService.getBestSellers(6));
		model.addAttribute("newest", bookService.getNewBooks(6));
		model.addAttribute("events", bookService.getEventBooks(6));
		
		return "mainpage/book";
	}
	
}