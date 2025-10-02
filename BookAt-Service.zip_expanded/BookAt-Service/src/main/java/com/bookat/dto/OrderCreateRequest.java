package com.bookat.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {

    @NotEmpty(message = "주문 상품을 선택해주세요.")
    @Valid
    private List<OrderItemCreateRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemCreateRequest {

        private String cartId;

        @NotBlank(message = "도서 ID는 필수입니다.")
        private String bookId;

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        private int price;

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private int quantity;
    }
}

