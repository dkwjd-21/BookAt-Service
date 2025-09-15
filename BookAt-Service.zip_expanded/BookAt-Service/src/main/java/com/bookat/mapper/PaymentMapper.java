package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookat.dto.PaymentDto;

@Mapper
public interface PaymentMapper {
    PaymentDto findById(Long paymentId);
    PaymentDto findByMerchantUid(String merchantUid);
    int insert(PaymentDto dto);
    int markPaidByMerchantUid(@Param("merchantUid") String merchantUid,
                              @Param("impUid") String impUid,
                              @Param("pgTid") String pgTid,
                              @Param("receiptUrl") String receiptUrl);
    int markFailedByMerchantUid(@Param("merchantUid") String merchantUid,
                                @Param("failReason") String failReason);
    
    int markCancelledByMerchantUid(@Param("merchantUid") String merchantUid);
    int markPartCancelledByMerchantUid(@Param("merchantUid") String merchantUid,
                                       @Param("receiptUrl") String receiptUrl);
}
