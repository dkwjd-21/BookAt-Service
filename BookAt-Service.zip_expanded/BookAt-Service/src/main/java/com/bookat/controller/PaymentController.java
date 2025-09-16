package com.bookat.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
  public String devNew(@RequestParam(defaultValue = "35000") Integer amount,
                       @RequestParam(defaultValue = "CARD") String method,
                       Model model) {

    // 서비스에서 CARD/VBANK/POINT로 정규화됨
    PaymentDto pay = paymentService.createReadyPayment(amount, method, "개발용 결제");
    model.addAttribute("merchantUid", pay.getMerchantUid());
    model.addAttribute("amount", pay.getPaymentPrice());
    return "payment/pay";
  }

  /** 프래그먼트 포함 페이지 테스트 */
  @GetMapping("/frag-test")
  public String fragTest(@RequestParam(defaultValue = "35000") Integer amount,
                         @RequestParam(defaultValue = "CARD") String method,
                         Model model) {

    // 서비스에서 CARD/VBANK/POINT로 정규화됨 (DB 제약: ('CARD','VBANK','POINT'))
    PaymentDto pay = paymentService.createReadyPayment(amount, method, "프래그먼트 테스트");
    model.addAttribute("merchantUid", pay.getMerchantUid());
    model.addAttribute("amount", pay.getPaymentPrice());
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
      String status   = (String) resp.get("status");       // "paid", "failed", ...
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

  /** 포트원 웹훅*/
  @PostMapping("/webhook")
  @ResponseBody
  public String webhook(@RequestBody Map<String, Object> payload) {
    try {
      // 1) 웹훅 기본 필드 꺼내기
      String impUid      = (String) payload.get("imp_uid");
      String merchantUid = (String) payload.get("merchant_uid");

      if (impUid == null || merchantUid == null) return "IGNORED";

      // 2) 서버에서 결제건 다시 조회
      String accessToken = portOneClient.getAccessToken().block();
      Map<String, Object> impPayment = portOneClient.getPaymentByImpUid(accessToken, impUid).block();
      Map<?, ?> resp = (Map<?, ?>) impPayment.get("response");

      String status   = (String) resp.get("status");      // paid / failed / cancelled ...
      String pgTid    = (String) resp.get("pg_tid");
      String receipt  = (String) resp.get("receipt_url"); // 취소건에도 들어올 수 있음
      String failReason = (String) resp.get("fail_reason");    // 실패 사유
      String cancelReason = (String) resp.get("cancel_reason"); // 취소 사유

      // 3) 상태별 분기 처리
      switch (status) {
        
        // 포트원에서 응답 상태 canceled 말고 cancelled로 내려줌
        case "cancelled":
          paymentService.markCanceled(
              merchantUid,
              (cancelReason != null ? cancelReason : (failReason != null ? failReason : "취소")),
              receipt
          );
          break;
        case "canceled":
            paymentService.markCanceled(
                merchantUid,
                (cancelReason != null ? cancelReason : (failReason != null ? failReason : "취소")),
                receipt
            );
            break;

        // 고객이 브라우저를 닫아버리거나 네트워크 끊겨도(=/complete까지 못 들어와도)
        // 웹훅(결제 성공이든, 취소든)은 도착가능 > DB의 결제 상태를 바꿀 수 있음
        case "paid":
          paymentService.markPaid(merchantUid, impUid, pgTid, receipt);
          break;

        case "failed":
          paymentService.markFailed(merchantUid, (failReason != null ? failReason : "결제실패"));
          break;

        default:
          break;
      }

      return "OK";
    } catch (Exception e) {

      return "웹훅에서 에러 발생";
    }
  }
}  