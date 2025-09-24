package com.bookat.controller;

import com.bookat.service.PaymentService;
import com.bookat.util.PaymentSessionStore;
import com.bookat.util.PortOneClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PortOneClient portOneClient;
    private final PaymentSessionStore sessionStore;

    // ----------------------------------------------------
    // 1) 이벤트 세션 시작: Hash에 필요한 모든 필드 저장
    // ----------------------------------------------------
    @PostMapping("/session/start-event")
    @ResponseBody
    public Map<String, Object> startEvent(@RequestParam String reservationToken,
                                          @RequestParam String eventId,
                                          @RequestParam String scheduleId,
                                          @RequestParam Integer reservedCount,
                                          @RequestParam Integer amount,
                                          @RequestParam String title,
                                          @RequestParam(required = false) String groupCounts, // JSON 문자열이면 그대로 저장
                                          @AuthenticationPrincipal(expression = "userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of("status", "error", "message", "unauthorized");
        }
        try {
            final String method = "CARD";
            var pay = paymentService.createReadyPayment(amount, method, title, userId);

            String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Map<String, Object> m = new HashMap<>();
            //공통 필수
            m.put("method", method);
            m.put("amount", String.valueOf(amount));
            m.put("merchantUid", pay.getMerchantUid());
            m.put("userId", userId);
            m.put("status", "READY");
            m.put("createdAt", now);
            m.put("title", title);
            m.put("impUid", "");           // 결제 완료 시 채울 수 있음
            m.put("qty", String.valueOf(reservedCount != null ? reservedCount : 1));
            m.put("bookId", "");           // 이벤트는 대표 bookId 없으면 비워두기

            //이벤트 전용도 공통 Hash에 저장
            m.put("reservationToken", reservationToken);
            m.put("eventId", eventId);
            m.put("scheduleId", scheduleId);
            m.put("reservedCount", String.valueOf(reservedCount));
            if (groupCounts != null) m.put("groupCounts", groupCounts);

            //주문 필드도 빈값으로 포함
            m.put("orderId", "");

            String token = sessionStore.create(m);

            return Map.of(
                    "status", "success",
                    "token", token,
                    "merchantUid", pay.getMerchantUid(),
                    "amount", amount,
                    "title", title,
                    "method", method
            );
        } catch (Exception e) {
            return Map.of("status", "error", "message", "failed_to_start");
        }
    }

    // ----------------------------------------------------
    // 2) 도서 세션 시작: Hash에 필요한 모든 필드 저장
    // ----------------------------------------------------
    @PostMapping("/session/start-order")
    @ResponseBody
    public Map<String, Object> startOrder(@RequestParam(required = false) Long orderId,
                                          @RequestParam Integer amount,
                                          @RequestParam String title,
                                          @RequestParam(required = false, defaultValue = "1") Integer qty,
                                          @AuthenticationPrincipal(expression = "userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of("status", "error", "message", "unauthorized");
        }
        try {
            final String method = "CARD";
            var pay = paymentService.createReadyPayment(amount, method, title, userId);

            String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Map<String, Object> m = new HashMap<>();
            //공통 필수
            m.put("method", method);
            m.put("amount", String.valueOf(amount));
            m.put("merchantUid", pay.getMerchantUid());
            m.put("userId", userId);
            m.put("status", "READY");
            m.put("createdAt", now);
            m.put("title", title);
            m.put("impUid", "");
            m.put("qty", String.valueOf(qty != null ? qty : 1));
            m.put("bookId", "");

            //주문
            m.put("orderId", orderId == null ? "" : String.valueOf(orderId));

            //이벤트 필드도 빈값으로 포함
            m.put("reservationToken", "");
            m.put("eventId", "");
            m.put("scheduleId", "");
            m.put("reservedCount", "0");
            m.put("groupCounts", "");

            String token = sessionStore.create(m);

            return Map.of(
                    "status", "success",
                    "token", token,
                    "merchantUid", pay.getMerchantUid(),
                    "amount", amount,
                    "title", title,
                    "method", method
            );
        } catch (Exception e) {
            return Map.of("status", "error", "message", "failed_to_start");
        }
    }

    // ----------------------------------------------------
    // 3) 세션 컨텍스트 조회
    // ----------------------------------------------------
    @GetMapping("/session/context")
    @ResponseBody
    public Map<String, Object> context(@RequestParam String token,
                                       @AuthenticationPrincipal(expression = "userId") String userId) {
        var h = sessionStore.getRaw(token, false); // take=false
        if (h == null) return Map.of("status", "error", "message", "session_invalid");
        if (!Objects.equals(String.valueOf(h.get("userId")), userId)) {
            return Map.of("status", "error", "message", "session_invalid");
        }
        int amount = Integer.parseInt(String.valueOf(h.get("amount")));
        String merchantUid = String.valueOf(h.get("merchantUid"));
        String title = String.valueOf(h.get("title"));
        String method = String.valueOf(h.get("method"));

        return Map.of(
                "status", "success",
                "merchantUid", merchantUid,
                "amount", amount,
                "title", title,
                "method", method,
                "token", token
        );
    }


    // ----------------------------------------------------
    // 4) 결제 완료 검증
    // ----------------------------------------------------
    @PostMapping("/api/complete")
    @ResponseBody
    public Map<String, Object> complete(@RequestBody Map<String, Object> body,
                                        @AuthenticationPrincipal(expression = "userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of("status", "error", "message", "unauthorized");
        }
        String token = String.valueOf(body.get("token"));
        String impUid = String.valueOf(body.get("impUid"));

        try {
            var h = sessionStore.getRaw(token, false); // ❌삭제 금지
            if (h == null) return Map.of("status", "error", "message", "session_invalid");
            if (!Objects.equals(String.valueOf(h.get("userId")), userId)) {
                return Map.of("status", "error", "message", "session_invalid");
            }

            String merchantUid = String.valueOf(h.get("merchantUid"));
            int expected = Integer.parseInt(String.valueOf(h.get("amount")));

            // PortOne 조회
            String accessToken = portOneClient.getAccessToken().block();
            var imp = portOneClient.getPaymentByImpUid(accessToken, impUid).block();
            @SuppressWarnings("unchecked")
            var resp = (java.util.Map<String, Object>) imp.get("response");
            int paidAmount = ((Number) resp.get("amount")).intValue();
            String status = String.valueOf(resp.get("status"));
            String pgTid = String.valueOf(resp.get("pg_tid"));
            String receipt = String.valueOf(resp.get("receipt_url"));

            var local = paymentService.findByMerchantUid(merchantUid);
            if (!"paid".equalsIgnoreCase(status) || local.getPaymentPrice() != paidAmount || expected != paidAmount) {
                paymentService.markFailed(merchantUid, "검증불일치/미결제");
                sessionStore.delete(token); // 실패 시 삭제
                return Map.of("status", "error", "message", "verify_failed");
            }

            paymentService.markPaid(merchantUid, impUid, pgTid, receipt);
            sessionStore.delete(token); // 성공 시 삭제

            return Map.of(
                    "status", "success",
                    "successRedirect", "/payment/success?m=" + merchantUid + "&i=" + impUid
            );
        } catch (Exception e) {
            try { sessionStore.delete(token); } catch (Exception ignore) {}
            return Map.of("status", "error", "message", "server_error");
        }
    }
    
    
    // ----------------------------------------------------
    // 5) 결제 성공 페이지
    // ----------------------------------------------------
    
    @GetMapping("/success")
    public String success(@RequestParam("m") String merchantUid,
                          @RequestParam(value = "i", required = false) String impUidParam,
                          Model model) {
        var pay = paymentService.findByMerchantUid(merchantUid);

        String impUid   = (impUidParam != null && !impUidParam.isBlank()) ? impUidParam : pay.getImpUid();
        String pgTid    = pay.getPgTid();
        String receipt  = pay.getReceiptUrl();

        try {
            boolean needFetch = (pgTid == null || pgTid.isBlank() || receipt == null || receipt.isBlank());
            if (needFetch && impUid != null && !impUid.isBlank()) {
                String accessToken = portOneClient.getAccessToken().block();
                @SuppressWarnings("unchecked")
                var resp = (java.util.Map<String, Object>) portOneClient.getPaymentByImpUid(accessToken, impUid).block().get("response");

                String fetchedPgTid   = String.valueOf(resp.get("pg_tid"));
                String fetchedReceipt = String.valueOf(resp.get("receipt_url"));

                if ((pgTid == null || pgTid.isBlank()) || (receipt == null || receipt.isBlank())) {
                    paymentService.markPaid(merchantUid, impUid, fetchedPgTid, fetchedReceipt);
                    pgTid = fetchedPgTid;
                    receipt = fetchedReceipt;
                }
            }
        } catch (Exception ignore) {
        } 

        model.addAttribute("merchantUid", merchantUid);
        model.addAttribute("impUid",      impUid);
        model.addAttribute("amount",      pay.getPaymentPrice());
        model.addAttribute("method",      pay.getPaymentMethod());
        model.addAttribute("status",      pay.getPaymentStatus());
        model.addAttribute("receiptUrl",  receipt);
        model.addAttribute("pgTid",       pgTid);

        return "payment/success";
    }
    

    // ----------------------------------------------------
    // 5) PortOne 웹훅
    // ----------------------------------------------------
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> body) {
        try {
            String impUid = String.valueOf(body.getOrDefault("imp_uid", body.get("impUid")));
            String merchantUid = String.valueOf(body.getOrDefault("merchant_uid", body.get("merchantUid")));
            if (impUid == null || impUid.isBlank()) return ResponseEntity.ok("ok");

            String accessToken = portOneClient.getAccessToken().block();
            var imp = portOneClient.getPaymentByImpUid(accessToken, impUid).block();
            @SuppressWarnings("unchecked")
            var resp = (java.util.Map<String, Object>) imp.get("response");
            int paidAmount = ((Number) resp.get("amount")).intValue();
            String status = String.valueOf(resp.get("status"));
            String pgTid = String.valueOf(resp.get("pg_tid"));
            String receipt = String.valueOf(resp.get("receipt_url"));

            var local = paymentService.findByMerchantUid(merchantUid);
            if ("paid".equalsIgnoreCase(status) && local.getPaymentPrice() == paidAmount) {
                paymentService.markPaid(merchantUid, impUid, pgTid, receipt);
            } else if ("cancelled".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) {
                paymentService.markFailed(merchantUid, "webhook:" + status);
            }
            return ResponseEntity.ok("ok");
        } catch (Exception ignore) {
            return ResponseEntity.ok("ok");
        }
    }
}
