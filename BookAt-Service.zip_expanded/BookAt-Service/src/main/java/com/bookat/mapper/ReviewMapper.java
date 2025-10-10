package com.bookat.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import com.bookat.dto.ReviewDto;

import com.bookat.entity.Review;


@Mapper
public interface ReviewMapper {
    List<ReviewDto> findByBookId(@Param("bookId") String bookId);
    int countByBookId(@Param("bookId") String bookId);

    List<Review> findByEventId(@Param("eventId") int eventId);
    List<ReviewDto> findReviewDtosByEventId(@Param("eventId") int eventId);
    int countByEventId(@Param("eventId") int eventId);
    
    // 리뷰 작성
    int insertReview(Review review);
    
    // 리뷰 수정
    int updateReview(Review review);
    
    // 리뷰 삭제
    int deleteReview(@Param("reviewId") int reviewId, @Param("userId") String userId);
    
    // 특정 사용자가 특정 도서에 작성한 리뷰 조회 (중복 체크용)
    Review findByBookIdAndUserId(@Param("bookId") String bookId, @Param("userId") String userId);
    
    // 특정 사용자가 특정 이벤트에 작성한 리뷰 조회 (중복 체크용)
    Review findByEventIdAndUserId(@Param("eventId") int eventId, @Param("userId") String userId);
    
    // 리뷰 ID로 조회
    Review findByReviewId(@Param("reviewId") int reviewId);

}