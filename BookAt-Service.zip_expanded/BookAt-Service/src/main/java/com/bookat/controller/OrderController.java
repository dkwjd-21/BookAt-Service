package com.bookat.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.BookDto;
import com.bookat.dto.CartResponse;
import com.bookat.dto.OrderCreateRequest;
import com.bookat.dto.OrderListItemResponse;
import com.bookat.dto.OrderStatusSummary;
import com.bookat.entity.Address;
import com.bookat.entity.User;
import com.bookat.service.AddressService;
import com.bookat.service.BookService;
import com.bookat.service.OrderService;

import jakarta.validation.Valid;


@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AddressService addressService;
    
    @Autowired
    private BookService bookService;

    @Value("${sweettracker.api.key:}")
    private String sweetTrackerApiKey;

    @GetMapping("/orderList")
    public String orderListPage(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("user", user);
        model.addAttribute("sweetTrackerApiKey", sweetTrackerApiKey);
        return "mypage/order_list";
    }

    @GetMapping("/orderList/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> orderListApi(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        List<OrderListItemResponse> orders = orderService.getOrderList(user.getUserId());
        OrderStatusSummary statusSummary = orderService.summarizeOrderStatus(orders);

        Map<String, Object> response = new HashMap<>();
        String userName = null;
        if (user != null) {
            userName = user.getUserName();
        }

        if (userName == null || userName.isBlank()) {
            userName = "북캣 회원";
        }

        response.put("success", true);
        response.put("userName", userName);
        response.put("orders", orders);
        response.put("statusSummary", statusSummary);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public String orderPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/user/login";
        }

        try {
            Address defaultAddress = null;
            try {
                defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            } catch (Exception addressException) {
                System.out.println("배송지 정보가 없습니다: " + addressException.getMessage());
            }
            
            model.addAttribute("user", user);
            model.addAttribute("address", defaultAddress);
            return "mypage/order";
        } catch (Exception e) {
            return "redirect:/user/login";
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderCreateRequest request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        try {
            Address defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            if (defaultAddress == null) {
                return ResponseEntity.badRequest().body("배송지 정보가 없습니다. 배송지를 먼저 등록해주세요.");
            }

            List<CartResponse> orderItems = request.getItems().stream()
                    .map(item -> CartResponse.builder()
                            .cartId(item.getCartId())
                            .bookId(item.getBookId())
                            .price(item.getPrice())
                            .cartQuantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());

            orderService.createOrder(user.getUserId(), orderItems, (long) defaultAddress.getAddrId());
            return ResponseEntity.ok().body("주문 생성 완료되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/address")
    public ResponseEntity<?> saveAddress(@RequestBody Map<String, Object> request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        try {
            String recipientName = (String) request.get("recipientName");
            String recipientPhone = (String) request.get("recipientPhone");
            String address = (String) request.get("address");

            if (recipientName == null || recipientPhone == null || address == null || 
                recipientName.trim().isEmpty() || recipientPhone.trim().isEmpty() || address.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("필수 정보를 모두 입력해주세요.");
            }

            Address newAddress = new Address();
            newAddress.setUserId(user.getUserId());
            newAddress.setAddrName("기본 배송지");
            newAddress.setRecipientName(recipientName);
            newAddress.setRecipientPhone(recipientPhone);
            newAddress.setAddr(address);

            Address existingAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            
            if (existingAddress != null) {
                existingAddress.setRecipientName(recipientName);
                existingAddress.setRecipientPhone(recipientPhone);
                existingAddress.setAddr(address);
                addressService.updateAddress(existingAddress);
            } else {
                addressService.saveAddress(newAddress);
            }

            return ResponseEntity.ok().body("배송지 정보가 저장되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("배송지 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @GetMapping("/direct")
    public String directOrderPage(@RequestParam String bookId, 
                                 @RequestParam int qty, 
                                 Model model, 
                                 @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/user/login";
        }

        try {
            System.out.println("바로구매 - 요청된 bookId: " + bookId);
            BookDto book = bookService.selectOne(bookId);
            System.out.println("바로구매 - 조회된 도서 정보: " + book);
            if (book == null) {
                System.out.println("바로구매 - 도서 정보가 null입니다. bookId: " + bookId);
                return "redirect:/books";
            }
            
            Address defaultAddress = null;
            try {
                defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            } catch (Exception addressException) {
                System.out.println("배송지 정보가 없습니다: " + addressException.getMessage());
            }
            
            model.addAttribute("user", user);
            model.addAttribute("address", defaultAddress);
            model.addAttribute("book", book);
            model.addAttribute("quantity", qty);
            model.addAttribute("isDirectOrder", true);
            
            return "mypage/order";
        } catch (Exception e) {
            return "redirect:/books";
        }
    }

    @GetMapping("/direct/api")
    public ResponseEntity<Map<String, Object>> getDirectOrderData(@RequestParam String bookId, 
                                                                 @RequestParam int qty, 
                                                                 @AuthenticationPrincipal User user) {
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(response);
        }

        try {
            BookDto book = bookService.selectOne(bookId);
            if (book == null) {
                response.put("success", false);
                response.put("message", "도서를 찾을 수 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            Address defaultAddress = null;
            try {
                defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            } catch (Exception addressException) {
                System.out.println("배송지 정보가 없습니다: " + addressException.getMessage());
            }
            
            response.put("success", true);
            response.put("user", user);
            response.put("address", defaultAddress);
            response.put("book", book);
            response.put("quantity", qty);
            response.put("isDirectOrder", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
