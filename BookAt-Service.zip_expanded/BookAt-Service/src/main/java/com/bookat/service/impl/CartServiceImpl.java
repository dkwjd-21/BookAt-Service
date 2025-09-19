package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
//CartServiceImpl.java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bookat.dto.CartResponse;
import com.bookat.mapper.CartMapper;
import com.bookat.service.CartService;
//... other imports

@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartMapper cartMapper;

	@Override
	public List<CartResponse> getCartItemsForCurrentUser(String userId) {
		// 컨트롤러에서 이미 인증된 사용자의 ID를 전달받았으므로, 그대로 매퍼에 전달합니다.
		return cartMapper.getCartItemsForCurrentUser(userId);
	}

	@Override
	public void deleteCartItem(String cartId) {
		cartMapper.deleteCartItem(cartId);
	}

	@Override
	public void updateCartItemQuantity(String cartId, int quantity) {
		cartMapper.updateCartItemQuantity(cartId, quantity);
	}

	@Override
	public void deleteCartItems(List<String> cartIds) {
		cartMapper.deleteCartItems(cartIds);
	}
}