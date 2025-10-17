package com.bookat.entity;

import java.time.LocalDate;

public class Review {
		private int reviewId;
	  	private int rating;     
	    private String userId;       
	    private LocalDate reviewDate;     
	    private String reviewContent;      
	    private String reviewTitle;
	    private char reviewType;  // 'B': 도서, 'E': 이벤트
	    private String bookId;
	    private Integer eventId;
	    
	    public Review() {}

		public Review(int reviewId, int rating, String userId, LocalDate reviewDate, String reviewContent,
				String reviewTitle, char reviewType, String bookId, Integer eventId) {
			super();
			this.reviewId = reviewId;
			this.rating = rating;
			this.userId = userId;
			this.reviewDate = reviewDate;
			this.reviewContent = reviewContent;
			this.reviewTitle = reviewTitle;
			this.reviewType = reviewType;
			this.bookId = bookId;
			this.eventId = eventId;
		}

		public int getReviewId() {
			return reviewId;
		}

		public void setReviewId(int reviewId) {
			this.reviewId = reviewId;
		}

		public int getRating() {
			return rating;
		}

		public void setRating(int rating) {
			this.rating = rating;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public LocalDate getReviewDate() {
			return reviewDate;
		}

		public void setReviewDate(LocalDate reviewDate) {
			this.reviewDate = reviewDate;
		}

		public String getReviewContent() {
			return reviewContent;
		}

		public void setReviewContent(String reviewContent) {
			this.reviewContent = reviewContent;
		}

		public String getReviewTitle() {
			return reviewTitle;
		}

	public void setReviewTitle(String reviewTitle) {
		this.reviewTitle = reviewTitle;
	}

	public char getReviewType() {
		return reviewType;
	}

	public void setReviewType(char reviewType) {
		this.reviewType = reviewType;
	}

	public String getBookId() {
		return bookId;
	}

	public void setBookId(String bookId) {
		this.bookId = bookId;
	}

	public Integer getEventId() {
		return eventId;
	}

	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}

	@Override
	public String toString() {
		return "Review [reviewId=" + reviewId + ", rating=" + rating + ", userId=" + userId + ", reviewDate="
				+ reviewDate + ", reviewContent=" + reviewContent + ", reviewTitle=" + reviewTitle 
				+ ", reviewType=" + reviewType + ", bookId=" + bookId + ", eventId=" + eventId + "]";
	};
	    
	    
	    
	    
}
