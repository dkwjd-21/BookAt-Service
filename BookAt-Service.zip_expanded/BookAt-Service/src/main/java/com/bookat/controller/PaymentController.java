package com.bookat.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.bookat.dto.PaymentCompleteRequest;
import com.bookat.dto.PaymentDto;
import com.bookat.dto.PaymentSession;
import com.bookat.dto.reservation.PaymentReservationSession;
import com.bookat.entity.User;
import com.bookat.service.BookService;
import com.bookat.service.EventService;
import com.bookat.service.PaymentService;
import com.bookat.util.PaymentSessionStore;
import com.bookat.util.PortOneClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final PortOneClient portOneClient;
  private final BookService bookService;
  private final PaymentSessionStore sessionStore;
  private final EventService eventService;

  /* 이벤트 결제 시작 (팝업 우측 요약에서 eventId/amount만 넘김)   */
  @PostMapping("/session/start-event")
  @ResponseBody
  public Map<String, Object> startEvent(@RequestParam Integer eventId,
                                        @RequestParam Integer amount,
                                        @RequestParam String title,
                                        @RequestParam(defaultValue = "CARD") String method,
                                        @AuthenticationPrincipal(expression = "userId") String userId) {
      if (userId == null || userId.isBlank()) return Map.of("status","error","message","unauthorized");

      String enforcedMethod = "CARD";
      var pay = paymentService.createReadyPayment(amount, enforcedMethod, title, userId);

      PaymentSession session = PaymentSessionStore.of(
          "EVENT:" + eventId, 1, enforcedMethod,
          java.math.BigDecimal.valueOf(amount),
          pay.getMerchantUid(), userId, title
      );
      String token = sessionStore.create(session);
      return Map.of("status","success","redirectUrl","/payment/frag-test?token=" + token);
  }
  
  
  //도서 세션
  @PostMapping("/session/start")
  @ResponseBody
  public Map<String, Object> start(@RequestParam String bookId,
                                   @RequestParam(defaultValue = "1") Integer qty,
                                   @RequestParam(defaultValue = "CARD") String method,
                                   @AuthenticationPrincipal(expression = "userId") String userId) {

    if (userId == null || userId.isBlank()) {
      return Map.of("status","error","message","unauthorized");
    }

    var book = bookService.selectOne(bookId);
    if (book == null) {
      return Map.of("status","error","message","invalid bookId");
    }

    int safeQty = (qty == null || qty < 1) ? 1 : qty;
    BigDecimal amount = book.getPrice().multiply(BigDecimal.valueOf(safeQty));
    String rawTitle = book.getTitle();
    String payTitle = safeQty > 1 ? rawTitle + " 외 " + (safeQty - 1) + "권" : rawTitle;

    // 무통장입금 실제 구현 x CARD로 강제
    String enforcedMethod = "CARD";

    var pay = paymentService.createReadyPayment(
          amount.intValueExact(), enforcedMethod, payTitle, userId);

    PaymentSession session = PaymentSessionStore.of(
    		"BOOK:" + bookId, safeQty, enforcedMethod, amount, pay.getMerchantUid(), userId, payTitle);

    String token = sessionStore.create(session);

    return Map.of("status","success","redirectUrl","/payment/frag-test?token=" + token);
  }

  @GetMapping("/session/context")
  @ResponseBody
  public Map<String, Object> context(@RequestParam String token,
                                     @AuthenticationPrincipal(expression = "userId") String userId) {
      if (userId == null || userId.isBlank()) {
          return Map.of("status","error","message","unauthorized");
      }
      // ★ 중요: 조회 전용이므로 소비하지 말 것(두 번째 인자를 false로)
      var ctx = sessionStore.get(token, false);
      if (ctx == null) {
          return Map.of("status","error","message","session_not_found");
      }
      if (!userId.equals(ctx.userId())) {
          return Map.of("status","error","message","forbidden");
      }
      return Map.of(
          "status","success",
          "merchantUid", ctx.merchantUid(),
          "amount", ctx.amount(),
          "title", ctx.title(),
          "method", ctx.method(),
          "token", token
      );
  }
  
  @GetMapping("/frag-test")
  public String fragTest(@RequestParam String token, Model model) {
      model.addAttribute("token", token); 
      return "payment/frag-test";
  }



/* 개발용: 페이지 전체 결제 테스트 
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
}*/
  @PostMapping("/api/complete")
  @ResponseBody
  public Map<String, Object> apiComplete(@RequestBody PaymentCompleteRequest req,
                                         @AuthenticationPrincipal(expression = "userId") String userId) {
      if (userId == null || userId.isBlank()) {
          return Map.of("status","error","message","unauthorized");
      }
      try {
          // 1) 세션 토큰 검증(세션은 소비하지 않음)
          var ctx = sessionStore.get(req.getToken(), false);
          if (ctx == null || !userId.equals(ctx.userId())) {
              return Map.of("status","error","message","session_invalid");
          }

          // 2) 포트원 조회/금액/상태 검증 (기존 로직 재사용)
          String accessToken = portOneClient.getAccessToken().block();
          var impPayment = portOneClient.getPaymentByImpUid(accessToken, req.getImpUid()).block();
          @SuppressWarnings("unchecked")
          var resp = (java.util.Map<String, Object>) impPayment.get("response");

          int paidAmount = ((Number) resp.get("amount")).intValue();
          String status   = (String) resp.get("status");
          String pgTid    = (String) resp.get("pg_tid");
          String receipt  = (String) resp.get("receipt_url");

          var local = paymentService.findByMerchantUid(req.getMerchantUid());
          if (!"paid".equals(status) || local.getPaymentPrice() != paidAmount) {
              paymentService.markFailed(req.getMerchantUid(), "검증불일치 또는 미결제");
              return Map.of("status","error","message","verify_failed");
          }

          // 3) 성공 처리
          paymentService.markPaid(req.getMerchantUid(), req.getImpUid(), pgTid, receipt);

          // 4) 프런트가 이동할 URL만 내려줌
          return Map.of(
              "status","success",
              "successRedirect", "/payment/success?m=" + req.getMerchantUid() + "&i=" + req.getImpUid()
          );

      } catch (Exception e) {
          paymentService.markFailed(req.getMerchantUid(), "서버오류: " + e.getMessage());
          return Map.of("status","error","message","server_error");
      }
  }
  
  @GetMapping("/success")
  public String success(@RequestParam("m") String merchantUid,
                        @RequestParam(value = "i", required = false) String impUid,
                        Model model) {
    var pay = paymentService.findByMerchantUid(merchantUid);
    if (pay == null) return "error/404";

    model.addAttribute("merchantUid", merchantUid);
    model.addAttribute("impUid", impUid);
    model.addAttribute("amount", pay.getPaymentPrice());
    model.addAttribute("method", pay.getPaymentMethod());
    model.addAttribute("status", pay.getPaymentStatus());
    model.addAttribute("receiptUrl", pay.getReceiptUrl());
    model.addAttribute("pgTid", pay.getPgTid());

    return "payment/success"; 
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
  
  	// 이벤트 예약 결제 과정
	@GetMapping("/{paymentToken}/paymentUI")
	public String renderPaymentFrag(@PathVariable String paymentToken, @RequestParam(name = "method", required = false) String requestMethod, @AuthenticationPrincipal User user, Model model) {
		
		String token = paymentToken.startsWith("payment:") ? paymentToken.substring("payment:".length()) : paymentToken;
		
		PaymentReservationSession session = sessionStore.getEventPay(token);
		
		if (session == null) {
			// 세션없음 : 만료/오류 페이지 -> 현재 페이지가 없어서 여기 진입하면 템플릿에러남
			return "error/404";
		}
		
		String userId = (user == null) ? null : user.getUserId();
		if (userId == null || !userId.equals(session.userId())) {
			// 세션없음 : 권한 없음 -> 현재 페이지가 없어서 여기 진입하면 템플릿에러남
			return "error/403";
		}
	    
		// 2) 프래그먼트에 필요한 값 모델로 주입 (서버 신뢰값만)
		String merchantUid = session.merchantUid();
		int totalPrice = session.amount().intValue();
		String method = (requestMethod != null && !requestMethod.isBlank()) ? requestMethod : (session.method() == null ? "CARD" : session.method());

		int reservedCount = session.reservedCount();
		int eventId = session.eventId();
		int scheduleId = session.scheduleId();
		String title = session.title();
		
	    model.addAttribute("merchantUid", merchantUid);
	    model.addAttribute("amount", totalPrice);
	    model.addAttribute("title", title);
	    model.addAttribute("method", method);
	    
	    model.addAttribute("reservedCount", reservedCount);
	    model.addAttribute("eventId", eventId);
	    model.addAttribute("scheduleId", scheduleId);
		
	    return "fragments/payFragment :: payFragment";
	}
	
	  @PostMapping("/api/complete_event")
	  @ResponseBody
	  public Map<String, Object> apiCompleteEventPay(@RequestBody PaymentCompleteRequest req, @AuthenticationPrincipal(expression = "userId") String userId) {
		  
		  if(userId == null || userId.isBlank()) {
			  return Map.of("status", "error", "message", "unauthorized");
		  }
		  
		  try {
			  PaymentReservationSession session = sessionStore.getEventPay(req.getToken());
			  if(session == null || !userId.equals(session.userId())) {
				  return Map.of("status", "error", "message", "session_invalid");
			  }
			  
			  String accessToken = portOneClient.getAccessToken().block();
			  var impPayment = portOneClient.getPaymentByImpUid(accessToken, req.getImpUid()).block();
			  
			  @SuppressWarnings("unchecked")
			  var resp = (Map<String, Object>) impPayment.get("response");
			  
	          int paidAmount = ((Number) resp.get("amount")).intValue();
	          String status   = (String) resp.get("status");
	          String pgTid    = (String) resp.get("pg_tid");
	          String receipt  = (String) resp.get("receipt_url");
	          
	          PaymentDto local = paymentService.findByMerchantUid(req.getMerchantUid());
	          if(!status.equals("paid") || local.getPaymentPrice() != paidAmount) {
	        	  paymentService.markFailed(req.getMerchantUid(), "검증 불일치 또는 미결제");
	        	  return Map.of("status", "error", "message", "verify_failed");
	          }
	          
	          paymentService.markPaid(req.getMerchantUid(), req.getImpUid(), pgTid, receipt);
	          
	          sessionStore.updateImpUid(req.getToken(), req.getImpUid());
	          
	          return Map.of("status", "success", "successRedirect", "/payment/success?m=" + req.getMerchantUid() + "&i=" + req.getImpUid());
		  } catch (Exception e) {
			  paymentService.markFailed(req.getMerchantUid(), "서버오류: " + e.getMessage());
			  return Map.of("status", "error", "message", "server_error");
		  }
		  
	  }
	  
  
}  