package com.bookat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookOrderRequestDto {
    // Mapper로 전달되어 INSERT에 사용될 DTO
    private long orderId; // selectKey를 통해 채워짐
    private int totalPrice;
    private String userId;
    // status, date 등은 DB에서 기본값으로 처리
}
