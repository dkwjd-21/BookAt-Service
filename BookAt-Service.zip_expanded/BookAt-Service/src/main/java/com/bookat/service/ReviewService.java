package com.bookat.service;
import java.util.List;
import com.bookat.dto.ReviewDto;

public interface ReviewService {
    List<ReviewDto> findByBookId(String bookId);
    int countByBookId(String bookId);
}