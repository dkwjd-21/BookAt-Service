// 나의 리뷰 초기화 함수
window.initMyReview = async function () {
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
      const response = await window.axiosInstance.get("/myPage/myReview/api");
      const data = response.data;

      if (!data?.success) {
        showEmptyReview();
        return;
      }

      const reviews = data.reviews || [];

      if (reviews.length === 0) {
        showEmptyReview();
        return;
      }

      // 정렬 적용
      const sortValue = sortSelect ? sortSelect.value : "latest";
      const sortedReviews = sortReviews(reviews, sortValue);

      // 리뷰 목록 렌더링
      renderReviews(sortedReviews);
    } catch (error) {
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
      <div class="review-item" data-review-id="${review.reviewId}" data-book-id="${review.bookId || ""}" data-event-id="${review.eventId || ""}">
        <div class="review-item-header">
          <span class="review-type-badge ${review.targetType === "도서" ? "book-type" : "event-type"}">
            ${review.targetType}
          </span>
          <div class="review-actions">
            <button class="review-action-btn edit-btn" data-review-id="${review.reviewId}">수정</button>
            <button class="review-action-btn delete-btn" data-review-id="${review.reviewId}">삭제</button>
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
              <span class="review-author">작성일</span>
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

    // 이벤트 위임 방식으로 변경 (중복 등록 방지)
    // 개별 버튼에 리스너를 추가하지 않고, 부모 요소에 한 번만 등록
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
};

// 전역 변수 (중복 선언 방지)
if (typeof window.currentEditReviewId === "undefined") {
  window.currentEditReviewId = null;
}
if (typeof window.currentDeleteReviewId === "undefined") {
  window.currentDeleteReviewId = null;
}

// 알림 모달 열기
window.showAlertModal = function (message, title = "알림") {
  const modal = document.getElementById("alertModal");
  const titleElement = document.getElementById("alertModalTitle");
  const messageElement = document.getElementById("alertModalMessage");

  if (titleElement) titleElement.textContent = title;
  if (messageElement) messageElement.textContent = message;

  if (modal) {
    modal.style.setProperty("display", "flex", "important");

    // 이벤트 리스너 중복 방지를 위해 once 옵션 사용
    // 모달 외부 클릭 시 닫기
    const handleOutsideClick = (e) => {
      if (e.target === modal) {
        window.closeAlertModal();
      }
    };
    modal.addEventListener("click", handleOutsideClick, { once: true });

    // ESC 키로 닫기
    const handleEscKey = (e) => {
      if (e.key === "Escape") {
        window.closeAlertModal();
      }
    };
    document.addEventListener("keydown", handleEscKey, { once: true });
  }
};

// 알림 모달 닫기
window.closeAlertModal = function () {
  const modal = document.getElementById("alertModal");
  if (modal) {
    modal.style.display = "none";
  }
};

// 수정 모달 열기
window.openEditModal = async function (reviewId) {
  try {
    // 현재 표시된 리뷰 목록에서 해당 리뷰 정보 찾기
    const reviewItem = document.querySelector(`[data-review-id="${reviewId}"]`);
    if (!reviewItem) {
      alert("리뷰 정보를 찾을 수 없습니다.");
      return;
    }

    // 리뷰 목록에서 이미 표시된 정보 가져오기
    const title = reviewItem.querySelector(".review-title")?.textContent || "";
    const content = reviewItem.querySelector(".review-text")?.textContent || "";
    const filledStars = reviewItem.querySelectorAll(".star.filled").length;

    window.currentEditReviewId = reviewId;

    // 모달에 데이터 설정
    document.getElementById("editTitle").value = title;
    document.getElementById("editContent").value = content;

    // 별점 설정
    window.setStars("editStars", filledStars);

    // 글자 수 업데이트
    window.updateCharCount("editTitle", "editTitleCount", 50);
    window.updateCharCount("editContent", "editContentCount", 500);

    // 모달 표시
    const modal = document.getElementById("editModal");
    modal.style.setProperty("display", "flex", "important");
  } catch (error) {
    alert("리뷰 정보를 가져오는 중 오류가 발생했습니다.");
  }
};

// 수정 모달 닫기
window.closeEditModal = function () {
  const modal = document.getElementById("editModal");
  modal.style.display = "none";
  window.currentEditReviewId = null;

  // 폼 초기화
  document.getElementById("editReviewForm").reset();
};

// 삭제 모달 열기
window.openDeleteModal = function (reviewId) {
  window.currentDeleteReviewId = reviewId;
  const modal = document.getElementById("deleteModal");
  modal.style.setProperty("display", "flex", "important");
};

// 삭제 모달 닫기
window.closeDeleteModal = function () {
  const modal = document.getElementById("deleteModal");
  modal.style.display = "none";
  window.currentDeleteReviewId = null;
};

// 리뷰 삭제 확인
window.confirmDelete = async function () {
  if (!window.currentDeleteReviewId) return;

  try {
    // 리뷰 아이템에서 bookId 또는 eventId 가져오기
    const reviewItem = document.querySelector(`[data-review-id="${window.currentDeleteReviewId}"]`);
    const bookId = reviewItem?.getAttribute("data-book-id");
    const eventId = reviewItem?.getAttribute("data-event-id");

    let deleteUrl;
    if (bookId) {
      deleteUrl = `/books/${bookId}/reviews/${window.currentDeleteReviewId}`;
    } else if (eventId) {
      deleteUrl = `/events/${eventId}/reviews/${window.currentDeleteReviewId}`;
    } else {
      alert("리뷰 정보를 찾을 수 없습니다.");
      return;
    }

    const response = await window.axiosInstance.delete(deleteUrl);

    if (response.data.success) {
      window.closeDeleteModal();
      window.showAlertModal("리뷰가 삭제되었습니다.", "삭제 완료");
      // 리뷰 목록만 다시 로드
      if (typeof window.initMyReview === "function") {
        await window.initMyReview();
      }
    } else {
      alert("리뷰 삭제에 실패했습니다.");
    }
  } catch (error) {
    alert("리뷰 삭제 중 오류가 발생했습니다.");
  }
};

// 별점 설정 함수
window.setStars = function (containerId, rating) {
  const container = document.getElementById(containerId);
  const stars = container.querySelectorAll(".star");

  // 현재 선택된 별점을 컨테이너에 저장
  if (container) {
    container.dataset.currentRating = rating;
  }

  stars.forEach((star, index) => {
    if (index < rating) {
      star.classList.add("selected");
    } else {
      star.classList.remove("selected");
    }
  });
};

// 글자 수 업데이트 함수
window.updateCharCount = function (inputId, countId, maxLength) {
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
};

// DOMContentLoaded에서 자동 실행
document.addEventListener("DOMContentLoaded", async () => {
  // 현재 페이지 경로 확인
  const currentPath = window.location.pathname;
  const isMyPageMain = currentPath === "/myPage" || currentPath === "/myPage/";

  // myPageMain이 아닌 경우에만 aside 링크 이벤트 등록 (myPageMain에서는 myPage.js가 처리)
  if (!isMyPageMain) {
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
          window.location.href = "/user/login";
        }
      });
    }

    // 기존 myReview.html 페이지에서 자동 실행
    if (typeof window.initMyReview === "function") {
      await window.initMyReview();
    }
  }

  // 이벤트 위임: 리뷰 목록의 수정/삭제 버튼 처리 (중복 등록 방지)
  const reviewList = document.getElementById("reviewList");
  if (reviewList) {
    reviewList.addEventListener("click", (e) => {
      const target = e.target;

      // 수정 버튼 클릭
      if (target.classList.contains("edit-btn")) {
        const reviewId = target.getAttribute("data-review-id");
        if (reviewId) {
          window.openEditModal(parseInt(reviewId));
        }
      }

      // 삭제 버튼 클릭
      if (target.classList.contains("delete-btn")) {
        const reviewId = target.getAttribute("data-review-id");
        if (reviewId) {
          window.openDeleteModal(parseInt(reviewId));
        }
      }
    });
  }

  // 이벤트 리스너 설정
  // 수정 모달의 별점 클릭 이벤트
  const editStars = document.getElementById("editStars");
  if (editStars) {
    // 클릭 이벤트
    editStars.addEventListener("click", (e) => {
      if (e.target.classList.contains("star")) {
        const rating = parseInt(e.target.dataset.rating);
        window.setStars("editStars", rating);
      }
    });

    // 호버 이벤트 - 좌측부터 채워지도록
    const stars = editStars.querySelectorAll(".star");
    stars.forEach((star) => {
      star.addEventListener("mouseenter", (e) => {
        const rating = parseInt(e.target.dataset.rating);
        stars.forEach((s, index) => {
          if (index < rating) {
            s.classList.add("hover");
          } else {
            s.classList.remove("hover");
          }
        });
      });
    });

    // 마우스가 별점 영역을 벗어났을 때 hover 클래스 제거
    editStars.addEventListener("mouseleave", () => {
      stars.forEach((s) => s.classList.remove("hover"));
    });
  }

  // 수정 모달의 입력 필드 이벤트
  const editTitle = document.getElementById("editTitle");
  const editContent = document.getElementById("editContent");

  if (editTitle) {
    editTitle.addEventListener("input", () => {
      window.updateCharCount("editTitle", "editTitleCount", 50);
    });
  }

  if (editContent) {
    editContent.addEventListener("input", () => {
      window.updateCharCount("editContent", "editContentCount", 500);
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

      // 별점 가져오기 - 컨테이너에 저장된 값 사용
      const editStarsContainer = document.getElementById("editStars");
      const rating = parseInt(editStarsContainer?.dataset.currentRating) || 5;

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
        // 리뷰 아이템에서 bookId 또는 eventId 가져오기
        const reviewItem = document.querySelector(`[data-review-id="${window.currentEditReviewId}"]`);
        const bookId = reviewItem?.getAttribute("data-book-id");
        const eventId = reviewItem?.getAttribute("data-event-id");

        let updateUrl;
        if (bookId) {
          updateUrl = `/books/${bookId}/reviews/${window.currentEditReviewId}`;
        } else if (eventId) {
          updateUrl = `/events/${eventId}/reviews/${window.currentEditReviewId}`;
        } else {
          alert("리뷰 정보를 찾을 수 없습니다.");
          return;
        }

        const response = await window.axiosInstance.put(updateUrl, null, {
          params: {
            title: title,
            content: content,
            rating: parseInt(rating),
          },
        });

        if (response.data.success) {
          window.closeEditModal();
          window.showAlertModal("리뷰가 수정되었습니다.", "수정 완료");
          // 리뷰 목록만 다시 로드
          if (typeof window.initMyReview === "function") {
            await window.initMyReview();
          }
        } else {
          alert("리뷰 수정에 실패했습니다.");
        }
      } catch (error) {
        alert("리뷰 수정 중 오류가 발생했습니다.");
      }
    });
  }
});
