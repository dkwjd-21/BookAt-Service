package com.bookat.dto;

import java.time.LocalDate;


import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {
    private int rating;          // 별점(1~5)
    private String userId;       // USERS.USER_ID
    private LocalDate createdAt;     // 작성일
    private String content;      // 내용
}