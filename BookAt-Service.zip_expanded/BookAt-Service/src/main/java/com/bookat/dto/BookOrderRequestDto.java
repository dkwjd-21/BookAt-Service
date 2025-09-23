package com.bookat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookOrderRequestDto {
    // Mapper로 전달되어 INSERT에 사용될 DTO
    private Long orderId; // selectKey를 통해 채워짐
    private Integer totalPrice;
    private String userId;
    private Long addrId; // 배송지 ID 추가
    // status, date 등은 DB에서 기본값으로 처리
}
