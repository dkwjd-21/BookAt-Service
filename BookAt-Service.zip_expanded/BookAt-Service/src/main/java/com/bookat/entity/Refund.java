package com.bookat.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Refund {
  private Long refundId;       // (SEQ_REFUND.NEXTVAL)
  private Long paymentId;      // FK -> PAYMENT.payment_id
  
}



