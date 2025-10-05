package com.bookat.service;

import java.util.List;

public interface OrderService {
    void createOrder(String userId, List<String> cartIds, Long addrId);

    Long createDirectOrder(String userId, String bookId, int quantity, int price, Long addrId);
}
