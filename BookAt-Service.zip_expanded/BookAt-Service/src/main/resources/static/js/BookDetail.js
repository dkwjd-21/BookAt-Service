// 도서 상세페이지 JavaScript 기능들

// DOM이 로드된 후 실행
document.addEventListener("DOMContentLoaded", function () {
  // 리뷰 모달 기능
  (function () {
    const modal = document.getElementById("reviewModal");
    const openBtn = document.getElementById("openReviewModalBtn");

    if (!modal || !openBtn) return; // 요소가 없으면 종료

    const closeBtn = modal.querySelector(".close-btn");
    const starsWrap = document.getElementById("starsInput");
    const stars = Array.from(starsWrap.querySelectorAll(".star"));
    const ratingInput = document.getElementById("rating");
    const form = document.getElementById("reviewForm");

    const openModal = () => {
      modal.style.display = "flex";
    };
    const closeModal = () => {
      modal.style.display = "none";
    };

    openBtn.addEventListener("click", (e) => {
      e.preventDefault();
      openModal();
    });
    closeBtn.addEventListener("click", closeModal);
    modal.addEventListener("click", (e) => {
      if (e.target === modal) closeModal();
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") closeModal();
    });

    if (starsWrap) {
      starsWrap.addEventListener("click", (e) => {
        const star = e.target.closest(".star");
        if (!star) return;
        const val = Number(star.dataset.value);
        ratingInput.value = val;
        stars.forEach((s) => s.classList.toggle("selected", Number(s.dataset.value) <= val));
      });
    }

    if (form) {
      form.addEventListener("submit", (e) => {
        if (Number(ratingInput.value) < 1) {
          e.preventDefault();
          alert("별점을 선택해주세요.");
        }
      });
    }
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
});
