package com.bookat.service;
import java.util.List;
import com.bookat.dto.ReviewDto;

import com.bookat.entity.Review;


public interface ReviewService {
    List<ReviewDto> findByBookId(String bookId);
    int countByBookId(String bookId);

    List<Review> findByEventId(int eventId);
    List<ReviewDto> findReviewDtosByEventId(int eventId);
    int countByEventId(int eventId);
    
    // 리뷰 작성
    int insertReview(Review review);
    
    // 리뷰 수정
    int updateReview(Review review);
    
    // 리뷰 삭제
    int deleteReview(int reviewId, String userId);
    
    // 중복 체크 (해당 사용자가 해당 도서에 리뷰를 이미 작성했는지)
    boolean hasUserReviewedBook(String bookId, String userId);
    
    // 중복 체크 (해당 사용자가 해당 이벤트에 리뷰를 이미 작성했는지)
    boolean hasUserReviewedEvent(int eventId, String userId);
    
    // 리뷰 ID로 조회
    Review findByReviewId(int reviewId);
    
    // 사용자가 작성한 모든 리뷰 조회 (도서/이벤트 정보 포함)
    List<ReviewDto> findByUserId(String userId);
}


