package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookat.dto.PaymentDto;

@Mapper
public interface PaymentMapper {
	
	int insert(PaymentDto dto);
	
    PaymentDto findById(Long paymentId);
    
    PaymentDto findByMerchantUid(String merchantUid);
    
    int markPaidByMerchantUid(@Param("merchantUid") String merchantUid,
                              @Param("impUid") String impUid,
                              @Param("pgTid") String pgTid,
                              @Param("receiptUrl") String receiptUrl);
    
    int markFailedByMerchantUid(@Param("merchantUid") String merchantUid,
                                @Param("failReason") String failReason);
    
    int markCanceledByMerchantUid(@Param("merchantUid") String merchantUid,
            @Param("status") int status,
            @Param("reason") String reason,
            @Param("receiptUrl") String receiptUrl);

    int updateOrderIdByMerchantUid(@Param("merchantUid") String merchantUid,
                                   @Param("orderId") Long orderId);

}
