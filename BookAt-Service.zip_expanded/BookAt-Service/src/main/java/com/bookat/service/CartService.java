package com.bookat.service;

import java.util.List;

import com.bookat.dto.CartResponse;

public interface CartService {
	List<CartResponse> getCartItemsForCurrentUser(String userId);

	void deleteCartItem(String cartId);

	void updateCartItemQuantity(String cartId, int quantity);
}
