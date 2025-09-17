package com.bookat.service;

import com.bookat.dto.PaymentDto;

public interface PaymentService {
    PaymentDto createReadyPayment(Integer amount, String method, String info, String userId);
    void markPaid(String merchantUid, String impUid, String pgTid, String receiptUrl);
    void markFailed(String merchantUid, String reason);
    
    //취소
    void markCanceled(String merchantUid, String reason, String cancelReceiptUrl,  boolean partial);
    PaymentDto findByMerchantUid(String merchantUid);
}
