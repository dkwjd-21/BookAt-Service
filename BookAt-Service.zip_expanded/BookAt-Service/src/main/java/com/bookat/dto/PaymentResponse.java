package com.bookat.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

public final class PaymentResponse {

  @Data
  @AllArgsConstructor
  public static class Result {
    private String paymentstatus;      // PAID / FAIL / CANCELLED / PART_CANCELLED ...
    private Long orderId;
    private Long paymentId;
    private String receiptUrl;  // 성공 시
    private String reason;      // 실패/취소 사유
  }
  
  // 결제창 진입용 뷰
  @Data @AllArgsConstructor
  public static class CheckoutView {
    private Long orderId;
    private String merchantUid;
    private BigDecimal amount;
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
  }
}