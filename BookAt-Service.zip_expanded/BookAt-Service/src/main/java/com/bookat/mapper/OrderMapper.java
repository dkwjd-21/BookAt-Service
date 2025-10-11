package com.bookat.mapper;

import java.util.List;

import com.bookat.dto.BookOrderItemRequestDto;
import com.bookat.dto.BookOrderRequestDto;
import com.bookat.dto.OrderItemResponse;
import com.bookat.dto.UserOrderSummaryDto;

public interface OrderMapper {
    void insertOrder(BookOrderRequestDto orderRequest);
    void insertOrderItem(BookOrderItemRequestDto orderItemRequest);
    List<UserOrderSummaryDto> selectUserOrderSummaries(String userId);
    List<OrderItemResponse> selectOrderItemsByOrderId(Long orderId);
}
