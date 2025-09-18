package com.bookat.controller;


import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.bookat.service.BookService;
import com.bookat.service.ReviewService;
import com.bookat.service.EventService;
import com.bookat.dto.EventResDto;
import com.bookat.dto.*;
import lombok.RequiredArgsConstructor;



@Controller
@RequiredArgsConstructor
@RequestMapping("/books")
public class BookController{

	private final BookService bookService;
	private final ReviewService reviewService;
    private final EventService eventService;
	
	// 메인 : /books
	@GetMapping
	public String books(@RequestParam(value = "category", required = false) String category,
			            @RequestParam(value="bPage", defaultValue="0") int bPage,
                        @RequestParam(value="nPage", defaultValue="0") int nPage,
                        @RequestParam(value="ePage", defaultValue="0") int ePage,
			            Model model) {
		
        List<BookDto> books = null;
        
        //카테고리 선택 시
        if (category != null && !category.isBlank()) {
            if (GROUPS.containsKey(category)) {
                books = bookService.findByCategories(GROUPS.get(category));
            } else if (ALLOWED.contains(category)) {
                books = bookService.findByCategory(category);
            } else {
            	category = null;

            }
        }
        
		//카테 고리 미선택 시 메인 섹션(베스트/신간/이벤트)
		
        if(category == null) {
        int SIZE = 60;
		model.addAttribute("best",   bookService.getBestSellers(SIZE));
		model.addAttribute("newest", bookService.getNewBooks   (SIZE));
		model.addAttribute("events", bookService.getEventBooks (SIZE));
		}
    
        model.addAttribute("books", books);
        model.addAttribute("selectedCategory", category);
		
        return "mainpage/book";
	}
	
	//도서 상세 페이지
	@GetMapping("/{bookId}")
	public String bookDetail(@PathVariable String bookId, Model model) {
		BookDto book = bookService.selectOne(bookId);
		model.addAttribute("book", book);

	    List<EventResDto> events = eventService.selectByBookId(bookId); 
	    model.addAttribute("events", events != null ? events : java.util.List.of());
		
		
        List<ReviewDto> reviews = reviewService.findByBookId(bookId);
        int reviewCount = reviewService.countByBookId(bookId);
		
	    
	    model.addAttribute("reviews", reviews);
	    model.addAttribute("reviewCount", reviewCount);
		
		return "mainpage/bookdetail";
	}
	
	//선물하기 기능
	@PostMapping("/{bookId}/gift")
	public String gift(@PathVariable String bookId) {
		return  "redirect:/books/" + bookId;
	}
	
	//장바구니 기능
	@PostMapping("/{bookId}/cart")
	public String cart(@PathVariable String bookId) {
		return  "redirect:/books/" + bookId;
	}
	
	//구매하기
	@PostMapping("/{bookId}/order")
	public String order(@PathVariable String bookId,
			            @RequestParam("qty") Integer qty,
                        @AuthenticationPrincipal(expression = "userId") String userId) {
		
		  // 로그인 후 돌아올 위치
		  if (userId == null || userId.isBlank()) {
		    return "redirect:/user/Login?next=/books/" + bookId;
		  }

		  // 주문 나중에 넣고 일단 결제 페이지로 이동(method도 일단 카드 고정)
		  return "redirect:/payment/frag-test?bookId=" + bookId + "&qty=" + qty + "&method=CARD";
	}
	
	
	// 카테고리(단일)
	private static final Set<String> ALLOWED = Set.of(
			"FICTION","POETRY","ESSAY","HUMANITY","SELF-HELP","MAGAZINE","KIDS","HOBBY"
	);
	
	// 소설&시 묶음 카테고리
	private static final Map<String, List<String>> GROUPS = Map.of(
			"FICTION_POETRY", List.of("FICTION","POETRY")
	);
	
	
	
}