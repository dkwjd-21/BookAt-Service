package com.bookat.service;

import java.util.List;

public interface OrderService {
    void createOrder(String userId, List<String> cartIds);
}
