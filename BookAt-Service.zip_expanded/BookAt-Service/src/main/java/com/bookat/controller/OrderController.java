package com.bookat.controller;

import java.util.HashMap;
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
import com.bookat.service.BookService;
import com.bookat.mapper.UserLoginMapper;
import com.bookat.dto.BookDto;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AddressService addressService;
    
    @Autowired
    private UserLoginMapper userLoginMapper;
    
    @Autowired
    private BookService bookService;

    @GetMapping
    public String orderPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/user/login";
        }

        try {
            // 사용자의 기본 배송지 정보 가져오기 (없어도 페이지 접근 허용)
            Address defaultAddress = null;
            try {
                defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            } catch (Exception addressException) {
                // 배송지 정보가 없어도 페이지 접근 허용
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
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request, @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().body("로그인이 필요합니다.");
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> cartIds = (List<String>) request.get("cartIds");
            
            if (cartIds == null || cartIds.isEmpty()) {
                return ResponseEntity.badRequest().body("주문할 상품을 선택해주세요.");
            }

            // 사용자의 기본 배송지 ID 가져오기
            Address defaultAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            if (defaultAddress == null) {
                return ResponseEntity.badRequest().body("배송지 정보가 없습니다. 배송지를 먼저 등록해주세요.");
            }

            orderService.createOrder(user.getUserId(), cartIds, (long) defaultAddress.getAddrId());
            return ResponseEntity.ok().body("주문 생성 완료되었습니다.");
        } catch (Exception e) {
            e.printStackTrace(); // 디버깅을 위한 로그 추가
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

            // 새 주소 객체 생성
            Address newAddress = new Address();
            newAddress.setUserId(user.getUserId());
            newAddress.setAddrName("기본 배송지");
            newAddress.setRecipientName(recipientName);
            newAddress.setRecipientPhone(recipientPhone);
            newAddress.setAddr(address);

            // 기존 기본 주소가 있는지 확인
            Address existingAddress = addressService.getDefaultAddressByUserId(user.getUserId());
            
            if (existingAddress != null) {
                // 기존 주소가 있으면 업데이트
                existingAddress.setRecipientName(recipientName);
                existingAddress.setRecipientPhone(recipientPhone);
                existingAddress.setAddr(address);
                addressService.updateAddress(existingAddress);
            } else {
                // 기존 주소가 없으면 새로 저장
                addressService.saveAddress(newAddress);
            }

            return ResponseEntity.ok().body("배송지 정보가 저장되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("배송지 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 바로구매 페이지
    @GetMapping("/direct")
    public String directOrderPage(@RequestParam String bookId, 
                                 @RequestParam int qty, 
                                 Model model, 
                                 @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/user/login";
        }

        try {
            // 도서 정보 가져오기
            System.out.println("바로구매 - 요청된 bookId: " + bookId);
            BookDto book = bookService.selectOne(bookId);
            System.out.println("바로구매 - 조회된 도서 정보: " + book);
            if (book == null) {
                System.out.println("바로구매 - 도서 정보가 null입니다. bookId: " + bookId);
                return "redirect:/books";
            }
            
            // 사용자의 기본 배송지 정보 가져오기
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
            model.addAttribute("isDirectOrder", true); // 바로구매임을 표시
            
            return "mypage/order";
        } catch (Exception e) {
            return "redirect:/books";
        }
    }
    
    // 바로구매 데이터 API (JSON 응답)
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
            // 도서 정보 가져오기
            BookDto book = bookService.selectOne(bookId);
            if (book == null) {
                response.put("success", false);
                response.put("message", "도서를 찾을 수 없습니다.");
                return ResponseEntity.ok(response);
            }
            
            // 사용자의 기본 배송지 정보 가져오기
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

    @PostMapping("/direct/create")
    public ResponseEntity<?> createDirectOrder(@RequestBody Map<String, Object> request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.notFound().build();
    }
}
