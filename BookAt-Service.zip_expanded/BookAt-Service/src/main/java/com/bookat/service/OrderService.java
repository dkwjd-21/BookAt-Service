package com.bookat.service;

import java.util.List;

public interface OrderService {
    int createOrder(String userId, List<String> cartIds, Long addrId);
}
