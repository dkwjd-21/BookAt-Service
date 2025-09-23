package com.bookat.mapper;

import com.bookat.dto.BookOrderRequestDto;
import com.bookat.dto.BookOrderItemRequestDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    void insertOrder(BookOrderRequestDto orderRequest);
    void insertOrderItem(BookOrderItemRequestDto orderItemRequest);
}
