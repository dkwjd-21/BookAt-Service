package com.bookat.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookat.entity.Review;
import com.bookat.entity.User;
import com.bookat.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class ReviewController {
	
	private final ReviewService reviewService;
	
	/**
	 * 리뷰 중복 체크 (해당 사용자가 해당 도서에 리뷰를 이미 작성했는지)
	 */
	@GetMapping("/{bookId}/reviews/check")
	public ResponseEntity<?> checkReviewExists(
			@PathVariable String bookId,
			Authentication authentication) {
		
		Map<String, Object> response = new HashMap<>();
		
		// 로그인 체크
		if (authentication == null || !authentication.isAuthenticated()) {
			response.put("success", false);
			response.put("message", "로그인이 필요합니다.");
			response.put("needLogin", true);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		User user = (User) authentication.getPrincipal();
		String userId = user.getUserId();
		
		boolean hasReview = reviewService.hasUserReviewedBook(bookId, userId);
		
		response.put("success", true);
		response.put("hasReview", hasReview);
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 리뷰 작성
	 */
	@PostMapping("/{bookId}/reviews")
	public ResponseEntity<?> createReview(
			@PathVariable String bookId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam int rating,
			Authentication authentication) {
		
		Map<String, Object> response = new HashMap<>();
		
		// 로그인 체크
		if (authentication == null || !authentication.isAuthenticated()) {
			response.put("success", false);
			response.put("message", "로그인이 필요합니다.");
			response.put("needLogin", true);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		User user = (User) authentication.getPrincipal();
		String userId = user.getUserId();
		
		// 입력값 유효성 검사
		if (title == null || title.trim().isEmpty()) {
			response.put("success", false);
			response.put("message", "리뷰 제목을 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (title.length() > 50) {
			response.put("success", false);
			response.put("message", "리뷰 제목은 50자 이내로 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (content == null || content.trim().isEmpty()) {
			response.put("success", false);
			response.put("message", "리뷰 내용을 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (content.length() > 500) {
			response.put("success", false);
			response.put("message", "리뷰 내용은 500자 이내로 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (rating < 1 || rating > 5) {
			response.put("success", false);
			response.put("message", "별점은 1~5점 사이로 선택해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		
		// 중복 리뷰 체크
		if (reviewService.hasUserReviewedBook(bookId, userId)) {
			response.put("success", false);
			response.put("message", "이미 해당 도서에 리뷰를 작성하셨습니다.");
			response.put("isDuplicate", true);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		
		// 리뷰 생성
		Review review = new Review();
		review.setUserId(userId);
		review.setBookId(bookId);
		review.setReviewType('B'); // 도서 리뷰
		review.setReviewTitle(title);
		review.setReviewContent(content);
		review.setRating(rating);
		review.setEventId(null); // 도서 리뷰이므로 이벤트 ID는 null
		
		try {
			int result = reviewService.insertReview(review);
			if (result > 0) {
				response.put("success", true);
				response.put("message", "리뷰가 성공적으로 작성되었습니다.");
				log.info("리뷰 작성 성공 - userId: {}, bookId: {}", userId, bookId);
			} else {
				response.put("success", false);
				response.put("message", "리뷰 작성에 실패했습니다.");
			}
		} catch (Exception e) {
			log.error("리뷰 작성 중 오류 발생", e);
			response.put("success", false);
			response.put("message", "리뷰 작성 중 오류가 발생했습니다.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 리뷰 수정
	 */
	@PutMapping("/{bookId}/reviews/{reviewId}")
	public ResponseEntity<?> updateReview(
			@PathVariable String bookId,
			@PathVariable int reviewId,
			@RequestParam String title,
			@RequestParam String content,
			@RequestParam int rating,
			Authentication authentication) {
		
		Map<String, Object> response = new HashMap<>();
		
		// 로그인 체크
		if (authentication == null || !authentication.isAuthenticated()) {
			response.put("success", false);
			response.put("message", "로그인이 필요합니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		User user = (User) authentication.getPrincipal();
		String userId = user.getUserId();
		
		// 입력값 유효성 검사
		if (title == null || title.trim().isEmpty()) {
			response.put("success", false);
			response.put("message", "리뷰 제목을 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (title.length() > 50) {
			response.put("success", false);
			response.put("message", "리뷰 제목은 50자 이내로 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (content == null || content.trim().isEmpty()) {
			response.put("success", false);
			response.put("message", "리뷰 내용을 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (content.length() > 500) {
			response.put("success", false);
			response.put("message", "리뷰 내용은 500자 이내로 입력해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (rating < 1 || rating > 5) {
			response.put("success", false);
			response.put("message", "별점은 1~5점 사이로 선택해주세요.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		
		// 리뷰 존재 여부 및 작성자 확인
		Review existingReview = reviewService.findByReviewId(reviewId);
		if (existingReview == null) {
			response.put("success", false);
			response.put("message", "존재하지 않는 리뷰입니다.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}
		
		if (!existingReview.getUserId().equals(userId)) {
			response.put("success", false);
			response.put("message", "본인이 작성한 리뷰만 수정할 수 있습니다.");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
		}
		
		// 리뷰 수정
		Review review = new Review();
		review.setReviewId(reviewId);
		review.setUserId(userId);
		review.setReviewTitle(title);
		review.setReviewContent(content);
		review.setRating(rating);
		
		try {
			int result = reviewService.updateReview(review);
			if (result > 0) {
				response.put("success", true);
				response.put("message", "리뷰가 성공적으로 수정되었습니다.");
				log.info("리뷰 수정 성공 - reviewId: {}, userId: {}", reviewId, userId);
			} else {
				response.put("success", false);
				response.put("message", "리뷰 수정에 실패했습니다.");
			}
		} catch (Exception e) {
			log.error("리뷰 수정 중 오류 발생", e);
			response.put("success", false);
			response.put("message", "리뷰 수정 중 오류가 발생했습니다.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 리뷰 삭제
	 */
	@DeleteMapping("/{bookId}/reviews/{reviewId}")
	public ResponseEntity<?> deleteReview(
			@PathVariable String bookId,
			@PathVariable int reviewId,
			Authentication authentication) {
		
		Map<String, Object> response = new HashMap<>();
		
		// 로그인 체크
		if (authentication == null || !authentication.isAuthenticated()) {
			response.put("success", false);
			response.put("message", "로그인이 필요합니다.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
		
		User user = (User) authentication.getPrincipal();
		String userId = user.getUserId();
		
		// 리뷰 존재 여부 및 작성자 확인
		Review existingReview = reviewService.findByReviewId(reviewId);
		if (existingReview == null) {
			response.put("success", false);
			response.put("message", "존재하지 않는 리뷰입니다.");
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}
		
		if (!existingReview.getUserId().equals(userId)) {
			response.put("success", false);
			response.put("message", "본인이 작성한 리뷰만 삭제할 수 있습니다.");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
		}
		
		try {
			int result = reviewService.deleteReview(reviewId, userId);
			if (result > 0) {
				response.put("success", true);
				response.put("message", "리뷰가 성공적으로 삭제되었습니다.");
				log.info("리뷰 삭제 성공 - reviewId: {}, userId: {}", reviewId, userId);
			} else {
				response.put("success", false);
				response.put("message", "리뷰 삭제에 실패했습니다.");
			}
		} catch (Exception e) {
			log.error("리뷰 삭제 중 오류 발생", e);
			response.put("success", false);
			response.put("message", "리뷰 삭제 중 오류가 발생했습니다.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
		
		return ResponseEntity.ok(response);
	}
}

