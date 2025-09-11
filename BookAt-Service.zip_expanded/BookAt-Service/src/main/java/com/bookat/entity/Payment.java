package com.bookat.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Payment {
  private Long paymentId;
  private BigDecimal totalPrice;
  private BigDecimal paymentPrice;
  private String paymentMethod;     // CARD / VIRTUAL / POINT
  private Integer paymentStatus;    // 0 READY, 1 PAID, -1 FAILED, 2 CANCELLED, 3 PART_CANCELLED
  private Date paymentDate;         // DEFAULT SYSDATE
  private String paymentInfo;
  
  //아임포트/PG 검증/영수증용
   private String impUid;            // UNIQUE
   private String merchantUid;       // UNIQUE
   private String pgProvider;
   
}