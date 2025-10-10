document.addEventListener("DOMContentLoaded", async () => {
  // 마이페이지 aside에서 나의 리뷰 링크 클릭 이벤트
  const asideReviewLink = document.querySelector(".js-mypage-review-link");

  if (asideReviewLink) {
    asideReviewLink.addEventListener("click", async (event) => {
      event.preventDefault();
      try {
        const res = await window.axiosInstance.get("/myPage/myReview", {
          responseType: "text",
        });
        document.open();
        document.write(res.data);
        document.close();
        window.history.pushState({}, "", "/myPage/myReview");
      } catch (error) {
        console.error("나의 리뷰 페이지 로드 중 오류", error);
        window.location.href = "/user/login";
      }
    });
  }

  const loginSection = document.getElementById("loginSection");
  const reviewList = document.getElementById("reviewList");
  const emptyReview = document.getElementById("emptyReview");
  const sortSelect = document.getElementById("sortSelect");

  // 토큰 확인
  const accessToken = localStorage.getItem("accessToken");
  if (!accessToken) {
    showLoginRequired();
    return;
  }

  // 사용자 인증 확인
  if (typeof window.validateUser === "function") {
    try {
      await window.validateUser();
    } catch (err) {
      showLoginRequired();
      return;
    }
  }

  // 리뷰 목록 로드
  await loadReviews();

  // 정렬 옵션 변경 이벤트
  if (sortSelect) {
    sortSelect.addEventListener("change", () => {
      loadReviews();
    });
  }

  // 리뷰 목록 로드 함수
  async function loadReviews() {
    try {
      console.log("나의 리뷰 API 호출 시작");
      const response = await window.axiosInstance.get("/myPage/myReview/api");
      console.log("나의 리뷰 API 응답:", response.data);
      const data = response.data;

      if (!data?.success) {
        console.log("API 응답 실패:", data);
        showEmptyReview();
        return;
      }

      const reviews = data.reviews || [];
      console.log("불러온 리뷰 개수:", reviews.length);
      console.log("리뷰 데이터:", reviews);

      if (reviews.length === 0) {
        console.log("리뷰가 없어서 빈 리뷰 화면 표시");
        showEmptyReview();
        return;
      }

      // 정렬 적용
      const sortValue = sortSelect ? sortSelect.value : "latest";
      const sortedReviews = sortReviews(reviews, sortValue);
      console.log("정렬된 리뷰:", sortedReviews);

      // 리뷰 목록 렌더링
      renderReviews(sortedReviews);
    } catch (error) {
      console.error("리뷰 목록 로드 중 오류:", error);
      if (error.response?.status === 401 || error.response?.status === 403) {
        showLoginRequired();
      } else {
        showEmptyReview();
      }
    }
  }

  // 리뷰 정렬 함수
  function sortReviews(reviews, sortType) {
    const sorted = [...reviews];

    switch (sortType) {
      case "latest":
        return sorted.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      case "rating":
        return sorted.sort((a, b) => b.rating - a.rating);
      case "type":
        return sorted.sort((a, b) => a.targetType.localeCompare(b.targetType));
      default:
        return sorted;
    }
  }

  // 리뷰 목록 렌더링
  function renderReviews(reviews) {
    const reviewListElement = document.getElementById("reviewList");

    reviewListElement.innerHTML = reviews
      .map(
        (review) => `
      <div class="review-item" data-review-id="${review.reviewId}">
        <div class="review-item-header">
          <span class="review-type-badge ${review.targetType === "도서" ? "book-type" : "event-type"}">
            ${review.targetType}
          </span>
          <div class="review-actions">
            <button class="review-action-btn edit-btn" onclick="openEditModal(${review.reviewId})">수정</button>
            <button class="review-action-btn delete-btn" onclick="openDeleteModal(${review.reviewId})">삭제</button>
          </div>
        </div>
        
        <div class="review-item-content">
          <div class="target-info">
            <h3 class="target-title">${review.targetTitle}</h3>
          </div>
          
          <div class="review-content">
            <div class="review-rating">
              ${generateStars(review.rating)}
            </div>
            <h4 class="review-title">${review.title}</h4>
            <p class="review-text">${review.content}</p>
            <div class="review-meta">
              <span class="review-author">작성자</span>
              <span class="review-date">${formatDate(review.createdAt)}</span>
            </div>
          </div>
        </div>
      </div>
    `
      )
      .join("");

    // 섹션 표시
    loginSection.style.display = "none";
    emptyReview.style.display = "none";
    reviewList.style.display = "block";
  }

  // 별점 생성 함수
  function generateStars(rating) {
    let stars = "";
    for (let i = 1; i <= 5; i++) {
      if (i <= rating) {
        stars += '<span class="star filled">★</span>';
      } else {
        stars += '<span class="star empty">☆</span>';
      }
    }
    return stars;
  }

  // 날짜 포맷 함수
  function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  }

  // 로그인 필요 표시
  function showLoginRequired() {
    loginSection.style.display = "block";
    reviewList.style.display = "none";
    emptyReview.style.display = "none";
  }

  // 빈 리뷰 표시
  function showEmptyReview() {
    loginSection.style.display = "none";
    reviewList.style.display = "none";
    emptyReview.style.display = "block";
  }
});

// 전역 변수 (중복 선언 방지)
if (typeof window.currentEditReviewId === "undefined") {
  window.currentEditReviewId = null;
}
if (typeof window.currentDeleteReviewId === "undefined") {
  window.currentDeleteReviewId = null;
}

// 수정 모달 열기
async function openEditModal(reviewId) {
  try {
    const response = await window.axiosInstance.get(`/reviews/${reviewId}`);
    const review = response.data;

    if (!review) {
      alert("리뷰 정보를 가져올 수 없습니다.");
      return;
    }

    window.currentEditReviewId = reviewId;

    // 모달에 데이터 설정
    document.getElementById("editTitle").value = review.reviewTitle || "";
    document.getElementById("editContent").value = review.reviewContent || "";

    // 별점 설정
    setStars("editStars", review.rating);

    // 글자 수 업데이트
    updateCharCount("editTitle", "editTitleCount", 50);
    updateCharCount("editContent", "editContentCount", 500);

    // 모달 표시
    const modal = document.getElementById("editModal");
    modal.style.setProperty("display", "flex", "important");
  } catch (error) {
    console.error("리뷰 수정 모달 열기 중 오류:", error);
    alert("리뷰 정보를 가져오는 중 오류가 발생했습니다.");
  }
}

// 수정 모달 닫기
function closeEditModal() {
  const modal = document.getElementById("editModal");
  modal.style.display = "none";
  window.currentEditReviewId = null;

  // 폼 초기화
  document.getElementById("editReviewForm").reset();
}

// 삭제 모달 열기
function openDeleteModal(reviewId) {
  window.currentDeleteReviewId = reviewId;
  const modal = document.getElementById("deleteModal");
  modal.style.setProperty("display", "flex", "important");
}

// 삭제 모달 닫기
function closeDeleteModal() {
  const modal = document.getElementById("deleteModal");
  modal.style.display = "none";
  window.currentDeleteReviewId = null;
}

// 리뷰 삭제 확인
async function confirmDelete() {
  if (!window.currentDeleteReviewId) return;

  try {
    const response = await window.axiosInstance.delete(`/reviews/${window.currentDeleteReviewId}`);

    if (response.data.success) {
      alert("리뷰가 삭제되었습니다.");
      closeDeleteModal();
      // 페이지 새로고침 대신 목록 다시 로드
      location.reload();
    } else {
      alert("리뷰 삭제에 실패했습니다.");
    }
  } catch (error) {
    console.error("리뷰 삭제 중 오류:", error);
    alert("리뷰 삭제 중 오류가 발생했습니다.");
  }
}

// 별점 설정 함수
function setStars(containerId, rating) {
  const container = document.getElementById(containerId);
  const stars = container.querySelectorAll(".star");

  stars.forEach((star, index) => {
    if (index < rating) {
      star.classList.add("selected");
    } else {
      star.classList.remove("selected");
    }
  });
}

// 글자 수 업데이트 함수
function updateCharCount(inputId, countId, maxLength) {
  const input = document.getElementById(inputId);
  const countElement = document.getElementById(countId);

  if (input && countElement) {
    const currentLength = input.value.length;
    countElement.textContent = `${currentLength}/${maxLength}`;

    if (currentLength > maxLength * 0.9) {
      countElement.classList.add("error");
    } else if (currentLength > maxLength * 0.8) {
      countElement.classList.add("warning");
    } else {
      countElement.classList.remove("warning", "error");
    }
  }
}

// 이벤트 리스너 설정
document.addEventListener("DOMContentLoaded", () => {
  // 수정 모달의 별점 클릭 이벤트
  const editStars = document.getElementById("editStars");
  if (editStars) {
    editStars.addEventListener("click", (e) => {
      if (e.target.classList.contains("star")) {
        const rating = parseInt(e.target.dataset.rating);
        setStars("editStars", rating);
      }
    });
  }

  // 수정 모달의 입력 필드 이벤트
  const editTitle = document.getElementById("editTitle");
  const editContent = document.getElementById("editContent");

  if (editTitle) {
    editTitle.addEventListener("input", () => {
      updateCharCount("editTitle", "editTitleCount", 50);
    });
  }

  if (editContent) {
    editContent.addEventListener("input", () => {
      updateCharCount("editContent", "editContentCount", 500);
    });
  }

  // 수정 폼 제출 이벤트
  const editForm = document.getElementById("editReviewForm");
  if (editForm) {
    editForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      if (!window.currentEditReviewId) return;

      const title = document.getElementById("editTitle").value.trim();
      const content = document.getElementById("editContent").value.trim();
      const rating = document.querySelector("#editStars .star.selected")?.dataset.rating || 5;

      // 유효성 검사
      if (!title || !content) {
        alert("제목과 내용을 모두 입력해주세요.");
        return;
      }

      if (title.length > 50) {
        alert("제목은 50자를 초과할 수 없습니다.");
        return;
      }

      if (content.length > 500) {
        alert("내용은 500자를 초과할 수 없습니다.");
        return;
      }

      try {
        const response = await window.axiosInstance.put(`/reviews/${window.currentEditReviewId}`, {
          reviewTitle: title,
          reviewContent: content,
          rating: parseInt(rating),
        });

        if (response.data.success) {
          alert("리뷰가 수정되었습니다.");
          closeEditModal();
          // 페이지 새로고침 대신 목록 다시 로드
          location.reload();
        } else {
          alert("리뷰 수정에 실패했습니다.");
        }
      } catch (error) {
        console.error("리뷰 수정 중 오류:", error);
        alert("리뷰 수정 중 오류가 발생했습니다.");
      }
    });
  }
});
