
package com.bookat.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bookat.dto.ReviewDto;
import com.bookat.entity.Review;
import com.bookat.mapper.ReviewMapper;
import com.bookat.service.ReviewService;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewMapper reviewMapper;
    @Override public List<ReviewDto> findByBookId(String bookId){ return reviewMapper.findByBookId(bookId); }
    @Override public int countByBookId(String bookId){ return reviewMapper.countByBookId(bookId); }
	@Override public List<Review> findByEventId(int eventId) {return reviewMapper.findByEventId(eventId);}
	@Override public int countByEventId(int eventId) { return reviewMapper.countByEventId(eventId); }
}
