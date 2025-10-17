package com.bookat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.bookat.dto.BookOrderItemRequestDto;
import com.bookat.dto.BookOrderRequestDto;
import com.bookat.dto.OrderItemResponse;
import com.bookat.dto.UserOrderSummaryDto;

public interface OrderMapper {
    void insertOrder(BookOrderRequestDto orderRequest);
    void insertOrderItem(BookOrderItemRequestDto orderItemRequest);
    List<UserOrderSummaryDto> selectUserOrderSummaries(String userId);
    List<OrderItemResponse> selectOrderItemsByOrderId(Long orderId);
    
    // [지나 추가] 방금 생성된(상태 CREATED=0) 최신 주문의 orderId 조회 (해당 사용자 기준)
    Long selectLatestCreatedOrderIdByUser(@Param("userId") String userId);
}
