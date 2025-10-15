package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

	@Override
	public boolean addToCart(String userId, String bookId, int quantity) {
		try {
			// 이미 장바구니에 같은 도서가 있는지 확인
			int existingCount = cartMapper.checkCartItem(userId, bookId);
			
			if (existingCount > 0) {
				// 이미 있는 경우 수량만 증가
				cartMapper.updateExistingCartItem(userId, bookId, quantity);
			} else {
				// 새로운 아이템 추가
				String cartId = "CART" + System.currentTimeMillis(); // 간단한 ID 생성
				cartMapper.addToCart(cartId, userId, bookId, quantity);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}