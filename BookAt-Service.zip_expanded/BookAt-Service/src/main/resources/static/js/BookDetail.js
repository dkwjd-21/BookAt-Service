// 도서 상세페이지 JavaScript 기능들

// DOM이 로드된 후 실행
document.addEventListener("DOMContentLoaded", function () {
  // 현재 로그인한 사용자 정보 가져와서 수정/삭제 버튼 표시
  (async function () {
    // 토큰이 없으면 API 호출하지 않음
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
      console.log("비로그인 상태 - 토큰 없음");
      return;
    }

    try {
      const response = await window.axiosInstance.get("/auth/validate");
      const currentUserId = response.data.userId;

      // 모든 리뷰 컨테이너 순회
      const reviewContainers = document.querySelectorAll(".review-item-container");
      reviewContainers.forEach((container) => {
        const authorId = container.dataset.authorId;
        const actionsDiv = container.querySelector(".review-actions");

        // 본인이 작성한 리뷰인 경우 수정/삭제 버튼 표시
        if (currentUserId === authorId) {
          actionsDiv.style.display = "flex";
        }
      });
    } catch (error) {
      // 로그인하지 않았거나 인증 실패 시 버튼 표시 안 함
      console.log("비로그인 상태 또는 인증 실패");
    }
  })();

  // 리뷰 모달 기능
  (function () {
    const modal = document.getElementById("reviewModal");
    const openBtn = document.getElementById("openReviewModalBtn");
    const reviewLoginModal = document.getElementById("reviewLoginRequiredModal");
    const duplicateModal = document.getElementById("duplicateReviewModal");
    const createSuccessModal = document.getElementById("reviewCreateSuccessModal");

    if (!modal || !openBtn) return; // 요소가 없으면 종료

    const closeBtn = modal.querySelector(".close-btn");
    const starsWrap = document.getElementById("starsInput");
    const stars = Array.from(starsWrap.querySelectorAll(".star"));
    const ratingInput = document.getElementById("rating");
    const form = document.getElementById("reviewForm");
    const titleInput = document.getElementById("reviewTitle");
    const contentTextarea = document.getElementById("reviewContent");
    const titleCharCount = document.getElementById("titleCharCount");
    const contentCharCount = document.getElementById("contentCharCount");

    const openModal = () => {
      modal.style.display = "flex";
    };
    const closeModal = () => {
      modal.style.display = "none";
      // 폼 초기화
      form.reset();
      ratingInput.value = "0";
      stars.forEach((s) => s.classList.remove("selected"));
      // 문자 수 카운트 초기화
      updateCharCount(titleInput, titleCharCount, 50);
      updateCharCount(contentTextarea, contentCharCount, 500);
    };

    // 문자 수 카운트 업데이트 함수
    const updateCharCount = (inputElement, countElement, maxLength) => {
      const currentLength = inputElement.value.length;
      countElement.textContent = currentLength;

      // 스타일 변경
      countElement.parentElement.className = "char-count";
      if (currentLength > maxLength * 0.9) {
        countElement.parentElement.classList.add("warning");
      }
      if (currentLength >= maxLength) {
        countElement.parentElement.classList.add("error");
      }
    };

    const openLoginModal = () => {
      reviewLoginModal.style.display = "flex";
    };
    const closeLoginModal = () => {
      reviewLoginModal.style.display = "none";
    };

    const openDuplicateModal = () => {
      duplicateModal.style.display = "flex";
    };
    const closeDuplicateModal = () => {
      duplicateModal.style.display = "none";
    };

    const openCreateSuccessModal = () => {
      createSuccessModal.style.display = "flex";
    };
    const closeCreateSuccessModal = () => {
      createSuccessModal.style.display = "none";
    };

    // 리뷰 작성 버튼 클릭 - 로그인 체크 및 중복 체크
    openBtn.addEventListener("click", async (e) => {
      e.preventDefault();

      // URL에서 bookId 추출
      const pathParts = window.location.pathname.split("/");
      const bookId = pathParts[pathParts.length - 1];

      try {
        // 로그인 체크 및 중복 체크
        const response = await window.axiosInstance.get(`/books/${bookId}/reviews/check`);
        const data = response.data;

        if (data.success) {
          if (data.hasReview) {
            // 이미 리뷰 작성함
            openDuplicateModal();
          } else {
            // 리뷰 작성 가능
            openModal();
          }
        }
      } catch (error) {
        if (error.response && error.response.status === 401) {
          // 로그인 필요
          openLoginModal();
        } else {
          console.error("리뷰 체크 중 오류:", error);
          alert("오류가 발생했습니다. 다시 시도해주세요.");
        }
      }
    });

    closeBtn.addEventListener("click", closeModal);
    modal.addEventListener("click", (e) => {
      if (e.target === modal) closeModal();
    });

    // 별점 선택
    if (starsWrap) {
      starsWrap.addEventListener("click", (e) => {
        const star = e.target.closest(".star");
        if (!star) return;
        const val = Number(star.dataset.value);
        ratingInput.value = val;
        stars.forEach((s) => s.classList.toggle("selected", Number(s.dataset.value) <= val));
      });
    }

    // 문자 수 카운트 이벤트 리스너
    if (titleInput && titleCharCount) {
      titleInput.addEventListener("input", () => {
        updateCharCount(titleInput, titleCharCount, 50);
      });
    }

    if (contentTextarea && contentCharCount) {
      contentTextarea.addEventListener("input", () => {
        updateCharCount(contentTextarea, contentCharCount, 500);
      });
    }

    // 리뷰 작성 폼 제출
    if (form) {
      form.addEventListener("submit", async (e) => {
        e.preventDefault();

        if (Number(ratingInput.value) < 1) {
          alert("별점을 선택해주세요.");
          return;
        }

        // 제목 길이 검사
        const title = titleInput.value.trim();
        if (title.length === 0) {
          alert("리뷰 제목을 입력해주세요.");
          titleInput.focus();
          return;
        }
        if (title.length > 50) {
          alert("리뷰 제목은 50자 이내로 입력해주세요.");
          titleInput.focus();
          return;
        }

        // 내용 길이 검사
        const content = contentTextarea.value.trim();
        if (content.length === 0) {
          alert("리뷰 내용을 입력해주세요.");
          contentTextarea.focus();
          return;
        }
        if (content.length > 500) {
          alert("리뷰 내용은 500자 이내로 입력해주세요.");
          contentTextarea.focus();
          return;
        }

        const pathParts = window.location.pathname.split("/");
        const bookId = pathParts[pathParts.length - 1];

        const formData = new FormData(form);

        try {
          const response = await window.axiosInstance.post(`/books/${bookId}/reviews`, formData, {
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
            },
          });

          const data = response.data;

          if (data.success) {
            closeModal();
            openCreateSuccessModal();
          } else {
            alert(data.message || "리뷰 작성에 실패했습니다.");
          }
        } catch (error) {
          console.error("리뷰 작성 중 오류:", error);
          if (error.response && error.response.data && error.response.data.message) {
            alert(error.response.data.message);
          } else {
            alert("리뷰 작성 중 오류가 발생했습니다.");
          }
        }
      });
    }

    // 로그인 모달 이벤트
    if (reviewLoginModal) {
      const loginCloseBtn = reviewLoginModal.querySelector(".close-btn");
      const goToLoginBtn = document.getElementById("reviewGoToLoginBtn");
      const cancelLoginBtn = document.getElementById("reviewCancelLoginBtn");

      loginCloseBtn.addEventListener("click", closeLoginModal);
      goToLoginBtn.addEventListener("click", () => {
        window.location.href = "/user/login";
      });
      cancelLoginBtn.addEventListener("click", closeLoginModal);

      reviewLoginModal.addEventListener("click", (e) => {
        if (e.target === reviewLoginModal) closeLoginModal();
      });
    }

    // 중복 리뷰 모달 이벤트
    if (duplicateModal) {
      const duplicateCloseBtn = duplicateModal.querySelector(".close-btn");
      const closeDuplicateBtn = document.getElementById("closeDuplicateModalBtn");

      duplicateCloseBtn.addEventListener("click", closeDuplicateModal);
      closeDuplicateBtn.addEventListener("click", closeDuplicateModal);

      duplicateModal.addEventListener("click", (e) => {
        if (e.target === duplicateModal) closeDuplicateModal();
      });
    }

    // 작성 성공 모달 이벤트
    if (createSuccessModal) {
      const createSuccessCloseBtn = createSuccessModal.querySelector(".close-btn");
      const createOkBtn = document.getElementById("reviewCreateOkBtn");

      createSuccessCloseBtn.addEventListener("click", () => {
        closeCreateSuccessModal();
        window.location.reload();
      });
      createOkBtn.addEventListener("click", () => {
        closeCreateSuccessModal();
        window.location.reload();
      });

      createSuccessModal.addEventListener("click", (e) => {
        if (e.target === createSuccessModal) {
          closeCreateSuccessModal();
          window.location.reload();
        }
      });
    }

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        closeModal();
        closeLoginModal();
        closeDuplicateModal();
      }
    });
  })();

  // 장바구니 추가 기능
  (function () {
    const addToCartBtn = document.getElementById("addToCartBtn");
    const cartSuccessModal = document.getElementById("cartSuccessModal");
    const loginRequiredModal = document.getElementById("loginRequiredModal");
    const qtyInput = document.getElementById("qty");

    if (!addToCartBtn || !cartSuccessModal || !loginRequiredModal || !qtyInput) return;

    // 모달 관련 요소들
    const cartSuccessCloseBtn = cartSuccessModal.querySelector(".close-btn");
    const loginRequiredCloseBtn = loginRequiredModal.querySelector(".close-btn");
    const goToCartBtn = document.getElementById("goToCartBtn");
    const continueShoppingBtn = document.getElementById("continueShoppingBtn");
    const goToLoginBtn = document.getElementById("goToLoginBtn");
    const cancelLoginBtn = document.getElementById("cancelLoginBtn");

    // 모달 열기/닫기 함수들
    const openCartSuccessModal = () => {
      cartSuccessModal.style.display = "flex";
    };
    const closeCartSuccessModal = () => {
      cartSuccessModal.style.display = "none";
    };
    const openLoginRequiredModal = () => {
      loginRequiredModal.style.display = "flex";
    };
    const closeLoginRequiredModal = () => {
      loginRequiredModal.style.display = "none";
    };

    // 장바구니 추가 버튼 클릭 이벤트
    addToCartBtn.addEventListener("click", async (e) => {
      e.preventDefault();

      // URL에서 bookId 추출
      const pathParts = window.location.pathname.split("/");
      const bookId = pathParts[pathParts.length - 1]; // 마지막 부분이 bookId
      const quantity = parseInt(qtyInput.value) || 1;

      try {
        // 사용자 인증 확인
        await window.validateUser();

        // 인증 성공 시 장바구니 추가 API 호출
        const formData = new FormData();
        formData.append("qty", quantity);

        const response = await window.axiosInstance.post(`/books/${bookId}/cart/api`, formData, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        });

        const data = response.data;

        if (data.success) {
          openCartSuccessModal();
        } else {
          alert(data.message || "장바구니 추가에 실패했습니다.");
        }
      } catch (error) {
        // 인증 실패 시 로그인 필요 모달 표시
        openLoginRequiredModal();
      }
    });

    // 모달 이벤트 리스너들
    cartSuccessCloseBtn.addEventListener("click", closeCartSuccessModal);
    loginRequiredCloseBtn.addEventListener("click", closeLoginRequiredModal);

    cartSuccessModal.addEventListener("click", (e) => {
      if (e.target === cartSuccessModal) closeCartSuccessModal();
    });

    loginRequiredModal.addEventListener("click", (e) => {
      if (e.target === loginRequiredModal) closeLoginRequiredModal();
    });

    // 모달 버튼 이벤트들
    goToCartBtn.addEventListener("click", () => {
      window.location.href = "/cart";
    });

    continueShoppingBtn.addEventListener("click", () => {
      closeCartSuccessModal();
    });

    goToLoginBtn.addEventListener("click", () => {
      window.location.href = "/user/login";
    });

    cancelLoginBtn.addEventListener("click", () => {
      closeLoginRequiredModal();
    });

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        closeCartSuccessModal();
        closeLoginRequiredModal();
      }
    });
  })();

  // 바로구매 기능
  (function () {
    const directOrderBtn = document.getElementById("directOrderBtn");
    const qtyInput = document.getElementById("qty");
    const loginRequiredModal = document.getElementById("loginRequiredModal");

    if (!directOrderBtn || !qtyInput || !loginRequiredModal) return;

    // 로그인 필요 모달 관련 함수들
    const openLoginRequiredModal = () => {
      loginRequiredModal.style.display = "flex";
    };
    const closeLoginRequiredModal = () => {
      loginRequiredModal.style.display = "none";
    };

    // 로그인 필요 모달 이벤트 리스너들
    const loginRequiredCloseBtn = loginRequiredModal.querySelector(".close-btn");
    const goToLoginBtn = document.getElementById("goToLoginBtn");
    const cancelLoginBtn = document.getElementById("cancelLoginBtn");

    loginRequiredCloseBtn.addEventListener("click", closeLoginRequiredModal);
    goToLoginBtn.addEventListener("click", () => {
      window.location.href = "/user/login";
    });
    cancelLoginBtn.addEventListener("click", closeLoginRequiredModal);

    loginRequiredModal.addEventListener("click", (e) => {
      if (e.target === loginRequiredModal) closeLoginRequiredModal();
    });

    directOrderBtn.addEventListener("click", async (e) => {
      e.preventDefault();

      // URL에서 bookId 추출
      const pathParts = window.location.pathname.split("/");
      const bookId = pathParts[pathParts.length - 1];
      const quantity = parseInt(qtyInput.value) || 1;

      try {
        // 사용자 인증 확인
        await window.validateUser();

        // 바로구매 상품 정보를 세션스토리지에 저장
        const directOrderItem = {
          bookId: bookId,
          quantity: quantity,
          isDirectOrder: true,
        };
        sessionStorage.setItem("directOrderItem", JSON.stringify(directOrderItem));

        // 바로구매 데이터도 저장 (order.js에서 사용)
        // 현재 페이지에서 도서 정보 가져오기
        const bookTitle = document.querySelector(".detail-title").textContent;
        const bookAuthor = document.querySelector(".detail-meta span:nth-child(2)").textContent;
        const bookPrice = parseInt(document.querySelector(".price-number").textContent.replace(/[^0-9]/g, ""));
        const bookImage = document.querySelector(".detail-thumb").src;

        const directOrderData = {
          success: true,
          book: {
            bookId: bookId,
            title: bookTitle,
            author: bookAuthor,
            price: bookPrice,
            imageUrl: bookImage,
          },
          quantity: quantity,
          isDirectOrder: true,
        };
        sessionStorage.setItem("directOrderData", JSON.stringify(directOrderData));

        // 장바구니 주문하기와 동일한 방식: axiosInstance로 페이지 내용 가져와서 교체
        window.axiosInstance
          .get(`/order/direct?bookId=${bookId}&qty=${quantity}`)
          .then((response) => {
            // URL 변경
            history.pushState(null, null, `/order/direct?bookId=${bookId}&qty=${quantity}`);

            // 현재 페이지를 주문페이지 내용으로 교체
            document.open();
            document.write(response.data);
            document.close();
          })
          .catch((error) => {
            console.error("바로구매 페이지 요청 중 오류:", error);
            if (error.response && error.response.status === 401) {
              // 인증 실패 시 로그인 페이지로 이동
              localStorage.removeItem("accessToken");
              localStorage.removeItem("refreshToken");
              alert("세션이 만료되었습니다. 다시 로그인해주세요.");
              window.location.href = "/user/login";
            } else {
              alert("바로구매 페이지 접근 중 오류가 발생했습니다.");
            }
          });
      } catch (error) {
        // 인증 실패 시 로그인 필요 모달 표시
        openLoginRequiredModal();
      }
    });
  })();

  // 리뷰 수정 기능
  (function () {
    const editModal = document.getElementById("editReviewModal");
    const updateSuccessModal = document.getElementById("reviewUpdateSuccessModal");
    if (!editModal) return;

    const closeBtn = editModal.querySelector(".close-btn");
    const starsWrap = document.getElementById("editStarsInput");
    const stars = Array.from(starsWrap.querySelectorAll(".star"));
    const ratingInput = document.getElementById("editRating");
    const form = document.getElementById("editReviewForm");
    const reviewIdInput = document.getElementById("editReviewId");
    const titleInput = document.getElementById("editReviewTitle");
    const contentInput = document.getElementById("editReviewContent");
    const titleCharCount = document.getElementById("editTitleCharCount");
    const contentCharCount = document.getElementById("editContentCharCount");

    const openEditModal = (reviewId, title, content, rating) => {
      reviewIdInput.value = reviewId;
      titleInput.value = title;
      contentInput.value = content;
      ratingInput.value = rating;

      // 별점 표시
      stars.forEach((s) => s.classList.toggle("selected", Number(s.dataset.value) <= rating));

      // 문자 수 카운트 업데이트
      updateCharCount(titleInput, titleCharCount, 50);
      updateCharCount(contentInput, contentCharCount, 500);

      editModal.style.display = "flex";
    };

    const closeEditModal = () => {
      editModal.style.display = "none";
      form.reset();
      ratingInput.value = "0";
      stars.forEach((s) => s.classList.remove("selected"));
      // 문자 수 카운트 초기화
      updateCharCount(titleInput, titleCharCount, 50);
      updateCharCount(contentInput, contentCharCount, 500);
    };

    // 문자 수 카운트 업데이트 함수
    const updateCharCount = (inputElement, countElement, maxLength) => {
      const currentLength = inputElement.value.length;
      countElement.textContent = currentLength;

      // 스타일 변경
      countElement.parentElement.className = "char-count";
      if (currentLength > maxLength * 0.9) {
        countElement.parentElement.classList.add("warning");
      }
      if (currentLength >= maxLength) {
        countElement.parentElement.classList.add("error");
      }
    };

    const openUpdateSuccessModal = () => {
      updateSuccessModal.style.display = "flex";
    };
    const closeUpdateSuccessModal = () => {
      updateSuccessModal.style.display = "none";
    };

    // 수정 버튼 클릭 이벤트
    document.addEventListener("click", (e) => {
      if (e.target.classList.contains("edit-btn")) {
        const container = e.target.closest(".review-item-container");
        const reviewId = container.dataset.reviewId;
        const title = container.dataset.title;
        const content = container.dataset.content;
        const rating = container.dataset.rating;

        openEditModal(reviewId, title, content, rating);
      }
    });

    closeBtn.addEventListener("click", closeEditModal);
    editModal.addEventListener("click", (e) => {
      if (e.target === editModal) closeEditModal();
    });

    // 별점 선택
    if (starsWrap) {
      starsWrap.addEventListener("click", (e) => {
        const star = e.target.closest(".star");
        if (!star) return;
        const val = Number(star.dataset.value);
        ratingInput.value = val;
        stars.forEach((s) => s.classList.toggle("selected", Number(s.dataset.value) <= val));
      });
    }

    // 문자 수 카운트 이벤트 리스너
    if (titleInput && titleCharCount) {
      titleInput.addEventListener("input", () => {
        updateCharCount(titleInput, titleCharCount, 50);
      });
    }

    if (contentInput && contentCharCount) {
      contentInput.addEventListener("input", () => {
        updateCharCount(contentInput, contentCharCount, 500);
      });
    }

    // 리뷰 수정 폼 제출
    if (form) {
      form.addEventListener("submit", async (e) => {
        e.preventDefault();

        if (Number(ratingInput.value) < 1) {
          alert("별점을 선택해주세요.");
          return;
        }

        // 제목 길이 검사
        const title = titleInput.value.trim();
        if (title.length === 0) {
          alert("리뷰 제목을 입력해주세요.");
          titleInput.focus();
          return;
        }
        if (title.length > 50) {
          alert("리뷰 제목은 50자 이내로 입력해주세요.");
          titleInput.focus();
          return;
        }

        // 내용 길이 검사
        const content = contentInput.value.trim();
        if (content.length === 0) {
          alert("리뷰 내용을 입력해주세요.");
          contentInput.focus();
          return;
        }
        if (content.length > 500) {
          alert("리뷰 내용은 500자 이내로 입력해주세요.");
          contentInput.focus();
          return;
        }

        const pathParts = window.location.pathname.split("/");
        const bookId = pathParts[pathParts.length - 1];
        const reviewId = reviewIdInput.value;

        const formData = new FormData(form);

        try {
          const response = await window.axiosInstance.put(`/books/${bookId}/reviews/${reviewId}`, formData, {
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
            },
          });

          const data = response.data;

          if (data.success) {
            closeEditModal();
            openUpdateSuccessModal();
          } else {
            alert(data.message || "리뷰 수정에 실패했습니다.");
          }
        } catch (error) {
          console.error("리뷰 수정 중 오류:", error);
          if (error.response && error.response.data && error.response.data.message) {
            alert(error.response.data.message);
          } else {
            alert("리뷰 수정 중 오류가 발생했습니다.");
          }
        }
      });
    }

    // 수정 성공 모달 이벤트
    if (updateSuccessModal) {
      const updateSuccessCloseBtn = updateSuccessModal.querySelector(".close-btn");
      const updateOkBtn = document.getElementById("reviewUpdateOkBtn");

      updateSuccessCloseBtn.addEventListener("click", () => {
        closeUpdateSuccessModal();
        window.location.reload();
      });
      updateOkBtn.addEventListener("click", () => {
        closeUpdateSuccessModal();
        window.location.reload();
      });

      updateSuccessModal.addEventListener("click", (e) => {
        if (e.target === updateSuccessModal) {
          closeUpdateSuccessModal();
          window.location.reload();
        }
      });
    }

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeEditModal();
    });
  })();

  // 리뷰 삭제 기능
  (function () {
    const deleteConfirmModal = document.getElementById("reviewDeleteConfirmModal");
    const deleteSuccessModal = document.getElementById("reviewDeleteSuccessModal");
    let currentDeleteReviewId = null;

    const openDeleteConfirmModal = () => {
      deleteConfirmModal.style.display = "flex";
    };
    const closeDeleteConfirmModal = () => {
      deleteConfirmModal.style.display = "none";
      currentDeleteReviewId = null;
    };

    const openDeleteSuccessModal = () => {
      deleteSuccessModal.style.display = "flex";
    };
    const closeDeleteSuccessModal = () => {
      deleteSuccessModal.style.display = "none";
    };

    // 삭제 버튼 클릭 시 확인 모달 표시
    document.addEventListener("click", (e) => {
      if (e.target.classList.contains("delete-btn")) {
        const container = e.target.closest(".review-item-container");
        currentDeleteReviewId = container.dataset.reviewId;
        openDeleteConfirmModal();
      }
    });

    // 삭제 확인 모달 이벤트
    if (deleteConfirmModal) {
      const confirmCloseBtn = deleteConfirmModal.querySelector(".close-btn");
      const confirmBtn = document.getElementById("reviewDeleteConfirmBtn");
      const cancelBtn = document.getElementById("reviewDeleteCancelBtn");

      confirmCloseBtn.addEventListener("click", closeDeleteConfirmModal);
      cancelBtn.addEventListener("click", closeDeleteConfirmModal);

      deleteConfirmModal.addEventListener("click", (e) => {
        if (e.target === deleteConfirmModal) closeDeleteConfirmModal();
      });

      // 삭제 확인 버튼 클릭
      confirmBtn.addEventListener("click", async () => {
        const pathParts = window.location.pathname.split("/");
        const bookId = pathParts[pathParts.length - 1];

        try {
          const response = await window.axiosInstance.delete(`/books/${bookId}/reviews/${currentDeleteReviewId}`);
          const data = response.data;

          if (data.success) {
            closeDeleteConfirmModal();
            openDeleteSuccessModal();
          } else {
            closeDeleteConfirmModal();
            alert(data.message || "리뷰 삭제에 실패했습니다.");
          }
        } catch (error) {
          console.error("리뷰 삭제 중 오류:", error);
          closeDeleteConfirmModal();
          if (error.response && error.response.data && error.response.data.message) {
            alert(error.response.data.message);
          } else {
            alert("리뷰 삭제 중 오류가 발생했습니다.");
          }
        }
      });
    }

    // 삭제 성공 모달 이벤트
    if (deleteSuccessModal) {
      const deleteSuccessCloseBtn = deleteSuccessModal.querySelector(".close-btn");
      const deleteOkBtn = document.getElementById("reviewDeleteOkBtn");

      deleteSuccessCloseBtn.addEventListener("click", () => {
        closeDeleteSuccessModal();
        window.location.reload();
      });
      deleteOkBtn.addEventListener("click", () => {
        closeDeleteSuccessModal();
        window.location.reload();
      });

      deleteSuccessModal.addEventListener("click", (e) => {
        if (e.target === deleteSuccessModal) {
          closeDeleteSuccessModal();
          window.location.reload();
        }
      });
    }

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        closeDeleteConfirmModal();
        closeDeleteSuccessModal();
      }
    });
  })();
});
