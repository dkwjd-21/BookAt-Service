package com.bookat.controller;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookat.service.BookService;
import com.bookat.dto.*;
import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController{

	private final BookService bookService;
	
	// 메인 : /books
	@GetMapping
	public String booksHome(Model model) {
		
		model.addAttribute("best", bookService.getBestSellers(6));
		model.addAttribute("newest", bookService.getNewBooks(6));
		model.addAttribute("events", bookService.getEventBooks(6));
		
		return "mainpage/book";
	}
	
	// 카테고리(단일)
	private static final Set<String> ALLOWED = Set.of(
			"FICTION","POETRY","ESSAY","HUMANITY","SELF-HELP","MAGAZINE","KIDS","HOBBY"
	);
	
	// 소설&시 묶음 카테고리
	private static final Map<String, List<String>> GROUPS = Map.of(
			"FICTION_POETRY", List.of("FICTION","POETRY")
	);
	
	@GetMapping("/list")
	public String listBooks(
		@RequestParam(value = "category", required = false) String category, Model model) {
		
		List<BookDto> books;
		if(category == null || category.isBlank()) {
		   books = bookService.findAll();
		   category = null;
		}else if(GROUPS.containsKey(category)) {
			books = bookService.findByCategories(GROUPS.get(category));
		}else if(ALLOWED.contains(category)) {
			books = bookService.findByCategory(category);
		}else {
			books = bookService.findAll();
			category = null;
		}
		
		model.addAttribute("books", books);
		model.addAttribute("selectedCategory", category);
		
		return "mainpage/booklist";
	}
	
	
}