package com.bookat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderSummaryDto {
    private Long orderId;
    private LocalDateTime orderDate;
    private Integer orderStatus;
    private Integer totalPrice;
    private Integer shippingFee;
}
