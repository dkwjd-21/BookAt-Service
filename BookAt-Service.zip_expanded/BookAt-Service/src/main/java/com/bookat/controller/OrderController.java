package com.bookat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookat.entity.User;
import com.bookat.entity.Address;
import com.bookat.service.OrderService;
import com.bookat.service.AddressService;
import com.bookat.mapper.UserLoginMapper;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AddressService addressService;
    
    @Autowired
    private UserLoginMapper userLoginMapper;

    @GetMapping
    public String orderPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/user/login";
        }

        try {
            // 사용자의 기본 배송지 정보 가져오기
            Address defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            
            model.addAttribute("user", user);
            model.addAttribute("address", defaultAddress);
            return "mypage/order";
        } catch (Exception e) {
            return "redirect:/user/login";
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> cartIds = (List<String>) request.get("cartIds");
            
            // 배송비 정보 추출
            Integer subtotal = (Integer) request.get("subtotal");
            Integer shippingFee = (Integer) request.get("shippingFee");
            Integer totalAmount = (Integer) request.get("totalAmount");

            if (cartIds == null || cartIds.isEmpty()) {
                return ResponseEntity.badRequest().body("주문할 상품을 선택해주세요.");
            }


            orderService.createOrder(user.getUserId(), cartIds);
            return ResponseEntity.ok().body("주문이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
