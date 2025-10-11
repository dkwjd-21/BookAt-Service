package com.bookat.controller;

import java.util.List;
import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.CartResponse;
import com.bookat.entity.User;
import com.bookat.service.CartService;
import com.bookat.service.BookService;
import com.bookat.dto.BookListRes;

@Controller
@RequestMapping("/cart")

public class CartController {
	
	@Autowired
	private CartService cartService;

	@Autowired
	private BookService bookService;
	
	
    @GetMapping
    public String cartPage(Model model) {
        model.addAttribute("recommendations", bookService.getBestSellers(6));
        return "mypage/cart";
    }
	 
	 @GetMapping("/api")
	 @ResponseBody
	 public List<CartResponse> getCartItems(Authentication authentication) {
		 if (authentication == null) {
			 return null; // 사용자가 인증되지 않은 경우
		 }
		 User user = (User) authentication.getPrincipal();
		 return cartService.getCartItemsForCurrentUser(user.getUserId());
	 }

	@DeleteMapping("/api/{cartId}")
	public ResponseEntity<?> deleteCartItem(@PathVariable String cartId) {
		cartService.deleteCartItem(cartId);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/api/{cartId}")
	public ResponseEntity<?> updateCartItemQuantity(@PathVariable String cartId, @RequestBody Map<String, Integer> payload) {
		Integer quantity = payload.get("quantity");
		if (quantity == null) {
			return ResponseEntity.badRequest().body("Quantity is required.");
		}
		cartService.updateCartItemQuantity(cartId, quantity);
		return ResponseEntity.ok().build();
	}

	// 추천 도서 JSON (랜덤 6권)
	@GetMapping("/recommendations")
	@ResponseBody
	public List<BookListRes> recommendations() {
		return bookService.getBestSellers(6);
	}

}