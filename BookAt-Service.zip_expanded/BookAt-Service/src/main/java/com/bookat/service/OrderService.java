package com.bookat.service;

import java.util.List;

import com.bookat.dto.CartResponse;
import com.bookat.dto.OrderListItemResponse;
import com.bookat.dto.OrderStatusSummary;

public interface OrderService {
    void createOrder(String userId, List<CartResponse> items, Long addrId);
    List<OrderListItemResponse> getOrderList(String userId);
    OrderStatusSummary summarizeOrderStatus(List<OrderListItemResponse> orders);
}
