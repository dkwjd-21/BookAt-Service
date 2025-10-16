
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
	@Override public List<ReviewDto> findReviewDtosByEventId(int eventId) {return reviewMapper.findReviewDtosByEventId(eventId);}
	@Override public int countByEventId(int eventId) { return reviewMapper.countByEventId(eventId); }
	
	@Override
	public int insertReview(Review review) {
		return reviewMapper.insertReview(review);
	}
	
	@Override
	public int updateReview(Review review) {
		return reviewMapper.updateReview(review);
	}
	
	@Override
	public int deleteReview(int reviewId, String userId) {
		return reviewMapper.deleteReview(reviewId, userId);
	}
	
	@Override
	public boolean hasUserReviewedBook(String bookId, String userId) {
		Review existingReview = reviewMapper.findByBookIdAndUserId(bookId, userId);
		return existingReview != null;
	}
	
	@Override
	public boolean hasUserReviewedEvent(int eventId, String userId) {
		Review existingReview = reviewMapper.findByEventIdAndUserId(eventId, userId);
		return existingReview != null;
	}
	
	@Override
	public Review findByReviewId(int reviewId) {
		return reviewMapper.findByReviewId(reviewId);
	}
	
	@Override
	public List<ReviewDto> findByUserId(String userId) {
		return reviewMapper.findByUserId(userId);
	}
	
    @Override
    public double avgRatingByBookId(String bookId) {
        Double v = reviewMapper.avgRatingByBookId(bookId);
        return v != null ? v : 0.0;
    }

}
