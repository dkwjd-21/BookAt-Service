package com.bookat.dto;

import java.time.LocalDate;


import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {
    private int reviewId;        // 리뷰 ID
    private int rating;          // 별점(1~5)
    private String userId;       // USERS.USER_ID
    private LocalDate createdAt; // 작성일
    private String title;        // 제목
    private String content;      // 내용
    private String reviewType;   // 리뷰 타입 (B: 도서, E: 이벤트)
    private String bookId;       // 도서 ID
    private String eventId;      // 이벤트 ID
    private String targetTitle;  // 대상 제목 (도서 제목 또는 이벤트 이름)
    private String targetType;   // 대상 타입 ("도서" 또는 "이벤트")
}