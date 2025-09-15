package com.bookat.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.dto.CartResponse;
import com.bookat.service.impl.CartServiceImpl;

@Controller
@RequestMapping("/cart")

public class CartController {
	
	@Autowired
	private CartServiceImpl cartService;
	
	
	 @GetMapping
	    public String cartPage(Model model) {
	        List<CartResponse> cartItems = cartService.getCartItemsForCurrentUser("xodnr");
	        model.addAttribute("cartItems", cartItems);
	        return "mypage/cart";
	    }

}