package com.bookat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookOrderItemRequestDto {
    // Mapper로 전달되어 INSERT에 사용될 DTO
    private long orderItemId; // selectKey를 통해 채워짐
    private String bookId;
    private long orderId;
    private int quantity;
    private int price;
}
