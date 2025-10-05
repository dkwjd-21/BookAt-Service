package com.bookat.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.PaymentCompleteRequest;
import com.bookat.dto.PaymentDto;
import com.bookat.dto.PaymentSession;
import com.bookat.dto.reservation.PaymentReservationSession;
import com.bookat.entity.User;
import com.bookat.service.BookService;
import com.bookat.service.EventService;
import com.bookat.service.OrderService;
import com.bookat.service.PaymentService;
import com.bookat.util.PaymentSessionStore;
import com.bookat.util.PortOneClient;
import com.bookat.util.ReservationRedisUtil;

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
  private final ReservationRedisUtil reservationRedisUtil;
  private final EventService eventService;
  
  //도서 세션
  @PostMapping("/session/start")
  @ResponseBody
  public Map<String,Object> startBook(@RequestParam String bookId,
                                      @RequestParam(defaultValue="1") int qty,
                                      @AuthenticationPrincipal com.bookat.entity.User user) {
      if (user == null) return Map.of("status","error","message","unauthorized");

      var book = bookService.selectOne(bookId);                 // title/price 얻기
      if (book == null) return Map.of("status","error","message","book_not_found");  // BookService.selectOne 사용

      int amount = book.getPrice().intValue() * Math.max(qty, 1);
      String title = book.getTitle();                           // BookDto.title 사용

      // READY 생성(merchantUid 발급) → 결제세션 저장(토큰 생성)
      var pay = paymentService.createReadyPayment(amount, "CARD", title, user.getUserId());
      var session = new PaymentSession(
          bookId, qty, "CARD",
          java.math.BigDecimal.valueOf(amount),
          pay.getMerchantUid(), user.getUserId(),
          "READY",
          java.time.OffsetDateTime.now().toString(),
          title,
          true
      );
      String token = sessionStore.create(session);
      return Map.of("status","success","token", token);
  }
  
  @PostMapping("/session/start-cart")
  @ResponseBody
  public Map<String,Object> startCart(@RequestParam int amount,
                                      @RequestParam String title,
                                      @AuthenticationPrincipal com.bookat.entity.User user) {
      if (user == null) return Map.of("status","error","message","unauthorized");

      var pay = paymentService.createReadyPayment(amount, "CARD", title, user.getUserId());
      var session = new PaymentSession(
          null, 0, "CARD",
          java.math.BigDecimal.valueOf(amount),
          pay.getMerchantUid(), user.getUserId(),
          "READY",
          java.time.OffsetDateTime.now().toString(),
          title,
          false
      );
      String token = sessionStore.create(session);
      return Map.of("status","success","token", token);
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

          // 3) 성공 처리 및 주문 생성
          Long orderId = null;
          if (ctx.directOrder()) {
              orderId = handleDirectOrder(userId, ctx);
          }

          paymentService.markPaid(req.getMerchantUid(), req.getImpUid(), pgTid, receipt);

          return Map.of(
              "status","success",
              "successRedirect", "/payment/success?m=" + req.getMerchantUid() + "&i=" + req.getImpUid(),
              "orderId", orderId
          );

      } catch (Exception e) {
          paymentService.markFailed(req.getMerchantUid(), "서버오류: " + e.getMessage());
          return Map.of("status","error","message","server_error");
      }
  }

  private Long handleDirectOrder(String userId, PaymentSession session) {
    if (session.bookId() == null) {
      throw new IllegalStateException("바로구매 정보가 없습니다.");
    }

    var book = bookService.selectOne(session.bookId());
    if (book == null) {
      throw new IllegalStateException("도서를 찾을 수 없습니다.");
    }

    Address address = addressService.getDefaultAddressByUserId(userId);
    if (address == null) {
      throw new IllegalStateException("배송지 정보가 없습니다.");
    }

    int quantity = Math.max(session.qty(), 1);
    int price = book.getPrice().intValue();

    return orderService.createDirectOrder(userId, book.getBookId(), quantity, price, (long) address.getAddrId());
  }
  
  @GetMapping("/success")
  public String success(@RequestParam("m") String merchantUid,
                        @RequestParam(value="i", required = false) String impUid,
                        @RequestParam(value="t", required = false) String token,
                        Model model) {

    // 1) 결제 기본 정보
    PaymentDto pay = paymentService.findByMerchantUid(merchantUid);
    if (pay == null) return "error/404";

    model.addAttribute("merchantUid", merchantUid);
    model.addAttribute("impUid", impUid);
    model.addAttribute("amount", pay.getPaymentPrice());
    model.addAttribute("method", pay.getPaymentMethod());
    model.addAttribute("status", pay.getPaymentStatus() == 1 ? "paid" : "ready");
    model.addAttribute("receiptUrl", pay.getReceiptUrl());
    model.addAttribute("pgTid", pay.getPgTid());

    // 2) 화면 렌더에 필요한 기본값(나중에 주문 조회 메서드 보고 가져오기)
    model.addAttribute("items", java.util.Collections.emptyList()); 
    model.addAttribute("productTotal", 0);
    model.addAttribute("shippingFee", 0);
    model.addAttribute("usedPoint", 0);
    model.addAttribute("earnPoint", 0);
    model.addAttribute("orderDate", java.time.LocalDate.now());

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
  
  	// 이벤트 예약 결제창 진입
	@GetMapping("/{paymentToken}/paymentUI")

	public String renderPaymentFrag(@PathVariable String paymentToken, @RequestParam(name = "method", required = false) String requestMetohd, @RequestParam(name = "token", required = false) String reservationToken, @AuthenticationPrincipal User user, Model model) {
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

		// 예약 세션 토큰 값
		if (reservationToken != null && !reservationToken.isBlank()) {
			boolean updatePaymentToken = reservationRedisUtil.updatePaymentSessionToken(reservationToken, paymentToken);
			if(!updatePaymentToken) {
				log.warn("예약토큰 {} 에 이미 다른 결제세션이 매핑", reservationToken);
			}
		}
	    
		// 프래그먼트에 필요한 값 모델로 주입 (서버 신뢰값만)
		String method = (requestMetohd != null && !requestMetohd.isBlank()) ? requestMetohd : (session.method() == null ? "CARD" : session.method());
		
	    model.addAttribute("merchantUid", session.merchantUid());
	    model.addAttribute("amount", session.amount().intValue());
	    model.addAttribute("title", session.title());
	    model.addAttribute("method", method);
	    model.addAttribute("reservedCount", session.reservedCount());
	    model.addAttribute("eventId", session.eventId());
	    model.addAttribute("scheduleId", session.scheduleId());
		
	    return "fragments/payFragment :: payFragment";
	}
	
	// 이벤트 결제 성공 or 실패 응답
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
	 
	// 결제 완료 후 자동 호출 reservation 1건 + ticket N건을 생성
	// 결제 실패나 사용자가 중간에 닫은 경우엔 만료시간 TTL로 자연 삭제
	@PostMapping("/paid_after")
	@ResponseBody
	public Map<String, Object> paidAfter(@RequestBody Map<String, String> request) {
		String paymentToken = request.get("token");
		
		PaymentReservationSession session = sessionStore.getEventPay(paymentToken);
		
		if(session == null) {
			return Map.of("status", "error", "message", "세션이 존재하지 않음");
		}
		
		try {
			paymentService.completeEventPayment(paymentToken, session);
			
			return Map.of("status", "success");
		} catch(Exception e) {
			return Map.of("status", "error", "message", e);
		}
	}
  
}  