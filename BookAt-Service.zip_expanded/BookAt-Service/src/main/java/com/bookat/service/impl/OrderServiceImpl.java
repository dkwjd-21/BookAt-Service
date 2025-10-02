package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookat.dto.BookOrderRequestDto;
import com.bookat.dto.BookOrderItemRequestDto;
import com.bookat.dto.CartResponse;
import com.bookat.mapper.OrderMapper;
import com.bookat.mapper.CartMapper;
import com.bookat.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartMapper cartMapper;

    @Override
    @Transactional
    public int createOrder(String userId, List<String> cartIds, Long addrId) {
        // 1. 선택된 장바구니 아이템들을 조회
        List<CartResponse> cartItems = cartMapper.getCartItemsByIds(cartIds);

        // 2. 총 금액 계산
        int totalPrice = cartItems.stream()
                .mapToInt(item -> item.getPrice() * item.getCartQuantity())
                .sum();

        // 3. 주문 생성
        BookOrderRequestDto orderRequest = BookOrderRequestDto.builder()
                .totalPrice(totalPrice)
                .userId(userId)
                .addrId(addrId)
                .build();

        orderMapper.insertOrder(orderRequest);

        // 4. 주문 아이템들 생성
        for (CartResponse cartItem : cartItems) {
            BookOrderItemRequestDto orderItemRequest = BookOrderItemRequestDto.builder()
                    .bookId(cartItem.getBookId())
                    .orderId(orderRequest.getOrderId())
                    .quantity(cartItem.getCartQuantity())
                    .price(cartItem.getPrice())
                    .build();

            orderMapper.insertOrderItem(orderItemRequest);
        }

        // 5. 주문 완료된 장바구니 아이템들 삭제
        cartMapper.deleteCartItems(cartIds);
        
        // [지나 추가] 생성된 orderId 반환
        return (orderRequest.getOrderId() instanceof Number)
                ? ((Number) orderRequest.getOrderId()).intValue()
                : Integer.parseInt(String.valueOf(orderRequest.getOrderId()));
    }
}
