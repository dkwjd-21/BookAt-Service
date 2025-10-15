package com.bookat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

//@Profile({"dev","local"})   // ← 운영에서 자동 비활성화
@Controller
@RequestMapping("/payment/dev")
public class devController {

  @GetMapping("/success")
  public String success(Model model,
                        @RequestParam(required = false) Integer amount,
                        @RequestParam(defaultValue = "CARD") String method,
                        @RequestParam(defaultValue = "paid") String status) {

    // 샘플 아이템(필드: title, author, qty, price, imageUrl, bookId)
    Map<String,Object> i1 = new HashMap<>();
    i1.put("title","유럽 풋볼 스카우팅 리포트 2025-26 출간 기념");
    i1.put("author","BookAt Research");
    i1.put("qty",1); i1.put("price",19800);
    i1.put("imageUrl","https://shopping-phinf.pstatic.net/main_5560734/55607341811.20250703085633.jpg");
    i1.put("bookId","B0001");

    Map<String,Object> i2 = new HashMap<>();
    i2.put("title","함께한 시간들");
    i2.put("author","서지나");
    i2.put("qty",1); i2.put("price",12800);
    i2.put("imageUrl","https://shopping-phinf.pstatic.net/main_5368629/53686290980.20250322092310.jpg");
    i2.put("bookId","B0002");

    List<Map<String,Object>> items = List.of(i1, i2);

    int productTotal = items.stream()
        .mapToInt(m -> ((Integer)m.get("price")) * ((Integer)m.get("qty"))).sum();
    int shippingFee  = 3000;
    int usedPoint    = 0;
    int earnPoint    = (int)Math.floor((productTotal - usedPoint) * 0.01);
    int finalAmount  = (amount != null ? amount : productTotal + shippingFee - usedPoint);

    model.addAttribute("merchantUid", "PAY-DEV-000001");
    model.addAttribute("method", method);
    model.addAttribute("status", status);
    model.addAttribute("items", items);
    model.addAttribute("productTotal", productTotal);
    model.addAttribute("shippingFee", shippingFee);
    model.addAttribute("usedPoint", usedPoint);
    model.addAttribute("amount", finalAmount);
    model.addAttribute("earnPoint", earnPoint);
    model.addAttribute("pgTid", "TID-DEV-1234567890");
    model.addAttribute("impUid", "imp_1234567890");
    model.addAttribute("receiptUrl", null);
    model.addAttribute("orderDate", LocalDate.now());

    // 우리가 만든 success.html 템플릿 경로
    return "payment/success";
  }
}