package com.bookat.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.PaymentDto;
import com.bookat.service.PaymentService;

import com.bookat.util.PortOneClient;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController{
	
	  private final PaymentService paymentService;
	  private final PortOneClient portOneClient;

	  /** 개발용: 금액만 넣고 결제창 띄우기 */
	  @GetMapping("/dev/new")
	  public String devNew(@RequestParam(defaultValue="100") Integer amount,
	                       Model model) {
	    // TODO: 로그인/주문 연동 시 user/order 정보와 함께 생성
	    PaymentDto pay = paymentService.createReadyPayment(amount, "CARD", "테스트 결제");
	    model.addAttribute("merchantUid", pay.getMerchantUid());
	    model.addAttribute("amount", pay.getPaymentPrice());
	    return "payment/pay"; // 결제창 띄우는 템플릿
	  }
	  
	  

	  /** 클라이언트 콜백: imp_uid, merchant_uid 수신 → 서버검증 */
	  @PostMapping("/complete")
	  public String complete(@RequestParam String imp_uid,
	                         @RequestParam String merchant_uid,
	                         Model model) {
	    try {
	      // 1) 서버토큰 발급 + 결제조회
	      String accessToken = portOneClient.getAccessToken().block();
	      Map<String, Object> impPayment = portOneClient.getPaymentByImpUid(accessToken, imp_uid).block();

	      // 2) 금액/상태 검증
	      PaymentDto local = paymentService.findByMerchantUid(merchant_uid);
	      int paidAmount = ((Number)((Map)impPayment.get("response")).get("amount")).intValue();
	      String status   = (String)((Map)impPayment.get("response")).get("status"); // paid, failed 등
	      String pgTid    = (String)((Map)impPayment.get("response")).get("pg_tid");
	      String receipt  = (String)((Map)impPayment.get("response")).get("receipt_url");

	      if (!"paid".equals(status) || local.getPaymentPrice() != paidAmount) {
	        paymentService.markFailed(merchant_uid, "검증불일치 또는 미결제");
	        model.addAttribute("reason", "검증 실패(금액/상태)");
	        return "payment/fail";
	      }

	      // 3) 로컬 상태 업데이트 → 성공 페이지
	      paymentService.markPaid(merchant_uid, imp_uid, pgTid, receipt);
	      model.addAttribute("amount", paidAmount);
	      model.addAttribute("receiptUrl", receipt);
	      model.addAttribute("merchantUid", merchant_uid);
	      return "payment/success";

	    } catch (Exception e) {
	      paymentService.markFailed(merchant_uid, "서버오류: " + e.getMessage());
	      model.addAttribute("reason", "서버오류");
	      return "payment/fail";
	    }
	  }

	  
	  /** 포트원 웹훅 */
	  @PostMapping("/webhook")
	  @ResponseBody
	  public String webhook(@RequestBody Map<String,Object> payload){
	    // TODO: imp_uid, merchant_uid 받아 추가 검증/동기화
	    return "OK";
	  }
}	  