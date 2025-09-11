package com.bookat.service;
import java.util.List;
import com.bookat.dto.ReviewDto;
import com.bookat.entity.Review;

public interface ReviewService {
    List<ReviewDto> findByBookId(String bookId);
    int countByBookId(String bookId);
    List<Review> findByEventId(int eventId);
    int countByEventId(int eventId);
}

