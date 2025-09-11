package com.bookat.dto;

import lombok.Data;
import java.math.BigDecimal;

public final class PaymentRequest {

  @Data
  public static class Initiate {
    private Long orderId; // 주문 ID
  }

  @Data
  public static class Complete {
    private String impUid;       // 아임포트 결제 고유번호
    private String merchantUid;  // 가맹점 주문 고유번호(서버 발급)
  }

  @Data
  public static class Cancel {
    private Long paymentId;      
    private String impUid;
    private BigDecimal amount;   // null/0=전액, >0=부분
    private String reason;
  }
}