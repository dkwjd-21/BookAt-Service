package com.bookat.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemResponse {
    private String orderDate;
    private Integer orderStatus;
    private String statusLabel;
    private Integer totalPrice;
    private Integer shippingFee;
    private Integer totalQuantity;
    private Integer distinctBookCount;
    private String trackingNumber;
    private List<OrderItemResponse> items;
}
