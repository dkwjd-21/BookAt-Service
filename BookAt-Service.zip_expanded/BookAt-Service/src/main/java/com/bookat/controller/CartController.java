package com.bookat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.CartResponse;
import com.bookat.service.impl.CartServiceImpl;
import com.bookat.service.BookService;
import com.bookat.dto.BookListRes;

@Controller
@RequestMapping("/cart")

public class CartController {
	
	@Autowired
	private CartServiceImpl cartService;

	@Autowired
	private BookService bookService;
	
	
	 @GetMapping
	    public String cartPage(Model model) {
	        List<CartResponse> cartItems = cartService.getCartItemsForCurrentUser("xodnr");
	        model.addAttribute("cartItems", cartItems);
	        model.addAttribute("recommendations", bookService.getBestSellers(6));
	        return "mypage/cart";
	    }

	// 추천 도서 JSON (랜덤 6권)
	@GetMapping("/recommendations")
	@ResponseBody
	public List<BookListRes> recommendations() {
		return bookService.getBestSellers(6);
	}

}