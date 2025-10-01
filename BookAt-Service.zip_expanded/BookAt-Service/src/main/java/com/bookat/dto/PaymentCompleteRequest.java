package com.bookat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompleteRequest {
    /** 결제 세션 토큰(예약 토큰) */
    private String token;

    private String impUid;

    private String merchantUid;
  
    private String depositor;

    /** 현금영수증 신청 여부: "Y"|"N" */
    private String cashReceiptApply;

    private String cashReceiptType;

    private String cashReceiptNumber;
}
