package com.bookat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookat.dto.ReviewDto;
import com.bookat.entity.User;
import com.bookat.service.ReviewService;

@Controller
@RequestMapping("/myPage")
public class MyPageController {
    
    @Autowired
    private ReviewService reviewService;
    
    @GetMapping("/myReview")
    public String myReviewPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/user/login";
        }
        
        model.addAttribute("user", user);
        return "mypage/myReview";
    }
    
    @GetMapping("/myReview/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> myReviewApi(@AuthenticationPrincipal User user) {
        System.out.println("=== 나의 리뷰 API 호출 시작 ===");
        System.out.println("인증된 사용자: " + (user != null ? user.getUserId() : "null"));
        
        if (user == null) {
            System.out.println("사용자가 null - 401 응답");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다."
            ));
        }

        System.out.println("사용자 ID로 리뷰 조회 시작: " + user.getUserId());
        List<ReviewDto> reviews = reviewService.findByUserId(user.getUserId());
        System.out.println("조회된 리뷰 개수: " + (reviews != null ? reviews.size() : "null"));
        System.out.println("리뷰 데이터: " + reviews);
        
        String userName = user.getUserName();
        if (userName == null || userName.isBlank()) {
            userName = "북캣 회원";
        }

        Map<String, Object> response = Map.of(
                "success", true,
                "userName", userName,
                "reviews", reviews
        );

        System.out.println("응답 데이터: " + response);
        System.out.println("=== 나의 리뷰 API 호출 완료 ===");
        return ResponseEntity.ok(response);
    }
}