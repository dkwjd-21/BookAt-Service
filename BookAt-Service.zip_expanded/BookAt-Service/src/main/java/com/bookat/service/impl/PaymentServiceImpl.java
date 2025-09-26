package com.bookat.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookat.domain.PaymentStatus;
import com.bookat.dto.PaymentDto;
import com.bookat.dto.reservation.PaymentReservationSession;
import com.bookat.mapper.PaymentMapper;
import com.bookat.service.PaymentService;
import com.bookat.service.ReservationService;
import com.bookat.util.PaymentSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

  private final PaymentMapper paymentMapper;
  private final ReservationService reservationService;
  private final PaymentSessionStore sessionStore;


  private String normalizeMethod(String method) {
    if (method == null) return "CARD";
    String m = method.trim().toUpperCase().replace("-", "").replace("_", "");
    switch (m) {
      case "CARD":  return "CARD";
      case "VBANK": return "VBANK";
      case "POINT": return "POINT";
      default:      return "CARD";
    }
  }

  @Override
  public PaymentDto createReadyPayment(Integer amount, String method, String info, String userId){
	//서버에서 merchantUid 생성
    String merchantUid = "PAY-" + userId + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                          .format(LocalDateTime.now());
    
    PaymentDto dto = new PaymentDto();
    dto.setTotalPrice(amount);
    dto.setPaymentPrice(amount);
    dto.setPaymentMethod(normalizeMethod(method)); // 정규화
    dto.setPaymentStatus(PaymentStatus.READY.code);
    dto.setPaymentInfo(info);
    dto.setMerchantUid(merchantUid);
    
    paymentMapper.insert(dto);
    return paymentMapper.findByMerchantUid(merchantUid);
  }

  @Override
  public void markPaid(String merchantUid, String impUid, String pgTid, String receiptUrl){
    paymentMapper.markPaidByMerchantUid(merchantUid, impUid, pgTid, receiptUrl);
  }

  @Override
  public void markFailed(String merchantUid, String reason){
    paymentMapper.markFailedByMerchantUid(merchantUid, reason);
  }
  
  @Override
  public void markCanceled(String merchantUid, String reason, String cancelReceiptUrl, boolean partial) {
	int status = partial ? PaymentStatus.PART_CANCELED.code : PaymentStatus.CANCELED.code;
    paymentMapper.markCanceledByMerchantUid(merchantUid, reason, cancelReceiptUrl);
  }

  @Override
  public PaymentDto findByMerchantUid(String merchantUid){
    return paymentMapper.findByMerchantUid(merchantUid);
  }
  

  // 결제 완료 후
  public void completeEventPayment(String paymentToken, PaymentReservationSession session) {
	  String reservationToken = session.reservationToken();
	  
	  if(reservationToken == null) {
		  throw new IllegalArgumentException("예약 토큰이 없습니다.");
	  }
	  
	  Long paymentId = paymentMapper.findByMerchantUid(session.merchantUid()).getPaymentId();

	  // 예매, 티켓 저장 및 이벤트회차 잔여좌석 차감
	  reservationService.createReservation(reservationToken, paymentId);
	  
	  // 결제 세션 삭제
	  sessionStore.consumeEventPay(paymentToken);
  }
  
}
