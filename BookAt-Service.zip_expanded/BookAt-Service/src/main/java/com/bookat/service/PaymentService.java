package com.bookat.service;

import com.bookat.dto.PaymentDto;

public interface PaymentService {
    PaymentDto createReadyPayment(Integer amount, String method, String info /* TODO user/order */);
    void markPaid(String merchantUid, String impUid, String pgTid, String receiptUrl);
    void markFailed(String merchantUid, String reason);
    PaymentDto findByMerchantUid(String merchantUid);
}
