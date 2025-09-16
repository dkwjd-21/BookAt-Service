package com.bookat.entity;

import java.time.LocalDate;

public class Review {
		private int reviewId;
	  	private int rating;     
	    private String userId;       
	    private LocalDate reviewDate;     
	    private String reviewContent;      
	    private String reviewTitle;
	    
	    public Review() {}

		public Review(int reviewId, int rating, String userId, LocalDate reviewDate, String reviewContent,
				String reviewTitle) {
			super();
			this.reviewId = reviewId;
			this.rating = rating;
			this.userId = userId;
			this.reviewDate = reviewDate;
			this.reviewContent = reviewContent;
			this.reviewTitle = reviewTitle;
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

		@Override
		public String toString() {
			return "Review [reviewId=" + reviewId + ", rating=" + rating + ", userId=" + userId + ", reviewDate="
					+ reviewDate + ", reviewContent=" + reviewContent + ", reviewTitle=" + reviewTitle + "]";
		};
	    
	    
	    
	    
}
