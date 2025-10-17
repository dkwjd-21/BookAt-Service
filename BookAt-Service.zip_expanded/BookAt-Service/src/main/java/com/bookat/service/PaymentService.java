package com.bookat.service;

import java.util.List;

import com.bookat.dto.PaymentDto;
import com.bookat.dto.reservation.PaymentReservationSession;

public interface PaymentService {
    PaymentDto createReadyPayment(Integer amount, String method, String info, String userId, Long orderId);
    void markPaid(String merchantUid, String impUid, String pgTid, String receiptUrl);
    void markFailed(String merchantUid, String reason);
    
    //취소
    void markCanceled(String merchantUid, String reason, String ReceiptUrl,  boolean partial);
    PaymentDto findByMerchantUid(String merchantUid);
    
    // 결제완료 후 처리
    void completeEventPayment(String paymentToken, PaymentReservationSession session);
    
    // 결제 건 조회
    List<PaymentDto> findAllByUserId(String userId);
    
}
