package com.bookat.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookat.dto.BookOrderItemRequestDto;
import com.bookat.dto.BookOrderRequestDto;
import com.bookat.dto.CartResponse;
import com.bookat.dto.OrderItemResponse;
import com.bookat.dto.OrderListItemResponse;
import com.bookat.dto.OrderStatusSummary;
import com.bookat.dto.UserOrderSummaryDto;
import com.bookat.mapper.CartMapper;
import com.bookat.mapper.OrderMapper;
import com.bookat.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartMapper cartMapper;

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional(readOnly = true)
    public List<OrderListItemResponse> getOrderList(String userId) {
        List<UserOrderSummaryDto> summaries = orderMapper.selectUserOrderSummaries(userId);

        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }

        List<UserOrderSummaryDto> filteredSummaries = summaries.stream()
                .filter(summary -> summary.getOrderStatus() == null || summary.getOrderStatus() != 0)
                .collect(Collectors.toList());

        if (filteredSummaries.isEmpty()) {
            return List.of();
        }

        Map<Long, List<OrderItemResponse>> itemsByOrderId = new HashMap<>();
        for (UserOrderSummaryDto summary : filteredSummaries) {
            List<OrderItemResponse> items = orderMapper.selectOrderItemsByOrderId(summary.getOrderId());
            itemsByOrderId.put(summary.getOrderId(), items != null ? items : List.of());
        }

        return filteredSummaries.stream()
                .map(summary -> {
                    List<OrderItemResponse> items = itemsByOrderId.getOrDefault(summary.getOrderId(), List.of());
                    int shippingFee = calculateShippingFee(items);

                    return OrderListItemResponse.builder()
                            .orderId(summary.getOrderId())
                            .orderDate(summary.getOrderDate() != null ? summary.getOrderDate().format(ORDER_DATE_FORMATTER) : null)
                            .orderStatus(summary.getOrderStatus())
                            .statusLabel(convertStatus(summary.getOrderStatus()))
                            .totalPrice(summary.getTotalPrice())
                            .trackingNumber(summary.getTrackingNumber())
                            .shippingFee(shippingFee)
                            .items(items)
                            .totalQuantity(items.stream().mapToInt(OrderItemResponse::getQuantity).sum())
                            .distinctBookCount(items.size())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private int calculateShippingFee(List<OrderItemResponse> items) {
        int subtotal = items.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();

        if (subtotal == 0) {
            return 0;
        }

        return subtotal < 15000 ? 3000 : 0;
    }

    @Override
    public OrderStatusSummary summarizeOrderStatus(List<OrderListItemResponse> orders) {
        OrderStatusSummary summary = new OrderStatusSummary();
        if (orders == null) {
            return summary;
        }

        for (OrderListItemResponse order : orders) {
            Integer status = order.getOrderStatus();
            if (status == null) {
                continue;
            }
            switch (status) {
                case 0 -> summary.incrementCreated();
                case 1 -> summary.incrementPaid();
                case -1 -> summary.incrementFailed();
                case 2 -> summary.incrementCancelled();
                case -2 -> summary.incrementRefunded();
                case 3 -> summary.incrementFulfilled();
                case 4 -> summary.incrementShipping();
                default -> {
                }
            }
        }
        return summary;
    }

    private String convertStatus(Integer status) {
        if (status == null) {
            return "상태 미정";
        }
        return switch (status) {
            case 0 -> "결제 대기중";
            case 1 -> "결제완료";
            case -1 -> "결제실패";
            case 2 -> "취소완료";
            case -2 -> "환불완료";
            case 3 -> "배송완료";
            case 4 -> "배송중";
            default -> "기타";
        };
    }

    @Override
    @Transactional
    public void createOrder(String userId, List<CartResponse> items, Long addrId) {
        int totalPrice = items.stream()
                .mapToInt(item -> item.getPrice() * item.getCartQuantity())
                .sum();

        BookOrderRequestDto orderRequest = BookOrderRequestDto.builder()
                .totalPrice(totalPrice)
                .userId(userId)
                .addrId(addrId)
                .build();

        orderMapper.insertOrder(orderRequest);

        for (CartResponse item : items) {
            BookOrderItemRequestDto orderItemRequest = BookOrderItemRequestDto.builder()
                    .bookId(item.getBookId())
                    .orderId(orderRequest.getOrderId())
                    .quantity(item.getCartQuantity())
                    .price(item.getPrice())
                    .build();

            orderMapper.insertOrderItem(orderItemRequest);
        }
        List<String> cartIds = items.stream()
                .map(CartResponse::getCartId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toList());

        if (!cartIds.isEmpty()) {
            cartMapper.deleteCartItems(cartIds);
        }

    }
}
