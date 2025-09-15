package com.bookat.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookat.domain.PaymentStatus;
import com.bookat.dto.PaymentDto;
import com.bookat.mapper.PaymentMapper;
import com.bookat.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {
	  private final PaymentMapper paymentMapper;

	  @Override
	  public PaymentDto createReadyPayment(Integer amount, String method, String info){
	    String merchantUid = "PAY-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
	                          .format(LocalDateTime.now());
	    PaymentDto dto = new PaymentDto();
	    dto.setTotalPrice(amount);
	    dto.setPaymentPrice(amount);
	    dto.setPaymentMethod(method);
	    dto.setPaymentStatus(PaymentStatus.READY.code);
	    dto.setPaymentInfo(info);
	    dto.setMerchantUid(merchantUid);
	    paymentMapper.insert(dto);
	    return paymentMapper.findByMerchantUid(merchantUid);
	  }

	  @Override public void markPaid(String merchantUid, String impUid, String pgTid, String receiptUrl){
	    paymentMapper.markPaidByMerchantUid(merchantUid, impUid, pgTid, receiptUrl);
	  }
	  @Override public void markFailed(String merchantUid, String reason){
	    paymentMapper.markFailedByMerchantUid(merchantUid, reason);
	  }
	  @Override public PaymentDto findByMerchantUid(String merchantUid){
	    return paymentMapper.findByMerchantUid(merchantUid);
	  }
}
