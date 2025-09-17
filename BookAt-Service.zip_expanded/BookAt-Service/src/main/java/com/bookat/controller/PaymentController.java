package com.bookat.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.bookat.dto.PaymentDto;
import com.bookat.service.PaymentService;
import com.bookat.util.PortOneClient;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final PortOneClient portOneClient;


  
  /** 개발용: 페이지 전체 결제 테스트 */
  @GetMapping("/dev/new")
  public String devNew(@RequestParam Integer amount,
                       @RequestParam String method,
                       Model model,
                       @AuthenticationPrincipal(expression = "userId") String userId) {
	  

    PaymentDto pay = paymentService.createReadyPayment(amount, method, "개발용 결제",userId);
    model.addAttribute("merchantUid", pay.getMerchantUid());
    model.addAttribute("amount", pay.getPaymentPrice());
    model.addAttribute("userId", userId);
    return "payment/pay";
  }
  

  /** 프래그먼트 포함 페이지 테스트 */
  @GetMapping("/frag-test")
  public String fragTest(@RequestParam Integer amount,
                         @RequestParam String method,
                         Model model,
                         @AuthenticationPrincipal(expression = "userId") String userId) {
	  

    PaymentDto pay = paymentService.createReadyPayment(amount, method, "프래그먼트 테스트", userId);
    model.addAttribute("merchantUid", pay.getMerchantUid());
    model.addAttribute("amount", pay.getPaymentPrice());
    model.addAttribute("userId", userId);
    return "payment/frag-test";
  }

  /** 결제 완료 콜백 (IMP.request_pay 성공시) */
  @PostMapping("/complete")
  public String complete(@RequestParam String imp_uid,
                         @RequestParam String merchant_uid,
                         Model model) {
    try {
      // 1) 서버 토큰 + 결제 조회
      String accessToken = portOneClient.getAccessToken().block();
      Map<String, Object> impPayment = portOneClient.getPaymentByImpUid(accessToken, imp_uid).block();
      Map<?, ?> resp = (Map<?, ?>) impPayment.get("response");

      // 2) 로컬/원격 금액·상태 검증
      PaymentDto local = paymentService.findByMerchantUid(merchant_uid);
      int paidAmount = ((Number) resp.get("amount")).intValue();
      String status   = (String) resp.get("status");      
      String pgTid    = (String) resp.get("pg_tid");
      String receipt  = (String) resp.get("receipt_url");
      // String payMethodActual = (String) resp.get("pay_method"); // 필요 시 사용

      if (!"paid".equals(status) || local.getPaymentPrice() != paidAmount) {
        paymentService.markFailed(merchant_uid, "검증불일치 또는 미결제");
        model.addAttribute("reason", "검증 실패(금액/상태)");
        return "payment/fail";
      }

      // 3) 성공 처리
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

  /* 포트원 웹훅 */
  @PostMapping("/webhook")
  @ResponseBody
  public String webhook(@RequestBody Map<String, Object> payload) {
    try {
      String impUid      = (String) payload.get("imp_uid");
      String merchantUid = (String) payload.get("merchant_uid");
      if (impUid == null || merchantUid == null) return "IGNORED";

      String accessToken = portOneClient.getAccessToken().block();
      Map<String, Object> impPayment = portOneClient.getPaymentByImpUid(accessToken, impUid).block();

      @SuppressWarnings("unchecked")
      Map<String, Object> resp = (Map<String, Object>) impPayment.get("response");

      String status  = (String) resp.get("status");          // paid / failed / canceled / PART_CANCELED
      String pgTid   = (String) resp.get("pg_tid");
      String receipt = (String) resp.get("receipt_url");

      int amount        = ((Number) resp.get("amount")).intValue();
      int cancelAmount  = ((Number) resp.getOrDefault("cancel_amount", 0)).intValue();

      String failReason   = (String) resp.get("fail_reason");
      String cancelReason = (String) resp.get("cancel_reason");
      String reason = cancelReason != null ? cancelReason
                     : (failReason != null ? failReason : "취소");

      switch (status) {
        case "paid":
          paymentService.markPaid(merchantUid, impUid, pgTid, receipt);
          break;

        
        case "canceled":
        case "cancelled": {
          // 포트원에서 보내주는 상태값에는 part_canceled가 따로 없기 때문에 금액으로 비교해야함
          boolean partial = cancelAmount > 0 && cancelAmount < amount;    
          paymentService.markCanceled(merchantUid, reason, receipt, partial);
          break;
        }

        case "failed":
          paymentService.markFailed(merchantUid, failReason != null ? failReason : "결제실패");
          break;

        default:
          // no-op
      }
      return "OK";
    } catch (Exception e) {
      // 필요하면 로그 추가
      // log.error("[WEBHOOK] error", e);
      return "웹훅에서 에러 발생";
    }
  }
}  