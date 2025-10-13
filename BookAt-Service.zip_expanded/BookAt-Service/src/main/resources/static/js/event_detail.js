// 페이지의 모든 HTML 요소가 완전히 로드된 후 모든 스크립트를 실행합니다.

document.addEventListener("DOMContentLoaded", function () {
  // 현재 로그인한 사용자 정보 가져와서 수정/삭제 버튼 표시
  (async function () {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
      console.log("비로그인 상태 - 토큰 없음");
      return;
    }
    try {
      const response = await window.axiosInstance.get("/auth/validate");
      const currentUserId = response.data.userId;

      const reviewContainers = document.querySelectorAll(".review-item-container");
      reviewContainers.forEach((container) => {
        const authorId = container.dataset.authorId;
        const actionsDiv = container.querySelector(".review-actions");

        if (currentUserId === authorId) {
          actionsDiv.style.display = "flex";
        }
      });
    } catch (error) {
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

    if (!modal || !openBtn) return;

    const closeBtn = modal.querySelector(".close-btn");
    const starsWrap = document.getElementById("starsInput");
    const stars = Array.from(starsWrap.querySelectorAll(".star"));
    const ratingInput = document.getElementById("rating");
    const form = document.getElementById("reviewForm");
    const titleInput = document.getElementById("reviewTitle");
    const contentTextarea = document.getElementById("reviewContent");
    const titleCharCount = document.getElementById("titleCharCount");
    const contentCharCount = document.getElementById("contentCharCount");

    const closeModal = () => {
      modal.style.display = "none";
      form.reset();
      ratingInput.value = "0";
      stars.forEach((s) => s.classList.remove("selected"));
      updateCharCount(titleInput, titleCharCount, 50);
      updateCharCount(contentTextarea, contentCharCount, 500);
    };

    const openModal = () => {
      modal.style.setProperty("display", "flex", "important");
    };

    // 문자 수 카운트 업데이트 함수
    const updateCharCount = (inputElement, countElement, maxLength) => {
      const currentLength = inputElement.value.length;
      countElement.textContent = currentLength;

      countElement.parentElement.className = "char-count";
      if (currentLength > maxLength * 0.9) {
        countElement.parentElement.classList.add("warning");
      }
      if (currentLength >= maxLength) {
        countElement.parentElement.classList.add("error");
      }
    };

    // 별점 선택 로직
    stars.forEach((star) => {
      star.addEventListener("click", () => {
        const value = parseInt(star.dataset.value);
        ratingInput.value = value;
        stars.forEach((s, idx) => {
          if (idx < value) {
            s.classList.add("selected");
          } else {
            s.classList.remove("selected");
          }
        });
      });
    });

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

    openBtn.addEventListener("click", async () => {
      try {
        const eventId = document.querySelector('input[name="eventId"]').value;
        const checkRes = await window.axiosInstance.get(`/events/${eventId}/reviews/check`);

        if (checkRes.data.needLogin) {
          openLoginModal();
          return;
        }

        if (checkRes.data.hasReview) {
          openDuplicateModal();
          return;
        }

        openModal();
      } catch (error) {
        console.error("리뷰 체크 오류:", error);
        if (error.response?.status === 401) {
          openLoginModal();
        }
      }
    });

    closeBtn?.addEventListener("click", closeModal);
    modal?.addEventListener("click", (e) => {
      if (e.target === modal) closeModal();
    });

    // 리뷰 작성 폼 제출
    if (form) {
      form.addEventListener("submit", async (e) => {
        e.preventDefault();

        if (Number(ratingInput.value) < 1) {
          alert("별점을 선택해주세요.");
          return;
        }

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

        const eventId = document.querySelector('input[name="eventId"]').value;
        const formData = new FormData(form);

        try {
          const response = await window.axiosInstance.post(`/events/${eventId}/reviews`, formData, {
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
          });

          if (response.data.success) {
            closeModal();
            openCreateSuccessModal();
          } else if (response.data.isDuplicate) {
            closeModal();
            openDuplicateModal();
          } else {
            alert(response.data.message || "리뷰 작성에 실패했습니다.");
          }
        } catch (error) {
          console.error("리뷰 작성 오류:", error);
          if (error.response?.data?.message) {
            alert(error.response.data.message);
          } else {
            alert("리뷰 작성 중 오류가 발생했습니다.");
          }
        }
      });
    }

    // 로그인 필요 모달
    const openLoginModal = () => {
      reviewLoginModal.style.setProperty("display", "flex", "important");
    };
    const closeLoginModal = () => {
      reviewLoginModal.style.display = "none";
    };

    const openDuplicateModal = () => {
      duplicateModal.style.setProperty("display", "flex", "important");
    };
    const closeDuplicateModal = () => {
      duplicateModal.style.display = "none";
    };

    const openCreateSuccessModal = () => {
      createSuccessModal.style.setProperty("display", "flex", "important");
    };
    const closeCreateSuccessModal = () => {
      createSuccessModal.style.display = "none";
    };

    if (reviewLoginModal) {
      const loginCloseBtn = reviewLoginModal.querySelector(".close-btn");
      const goToLoginBtn = document.getElementById("reviewGoToLoginBtn");
      const cancelLoginBtn = document.getElementById("reviewCancelLoginBtn");

      loginCloseBtn?.addEventListener("click", closeLoginModal);
      goToLoginBtn?.addEventListener("click", () => {
        window.location.href = "/user/login";
      });
      cancelLoginBtn?.addEventListener("click", closeLoginModal);
      reviewLoginModal.addEventListener("click", (e) => {
        if (e.target === reviewLoginModal) closeLoginModal();
      });
    }

    if (duplicateModal) {
      const dupCloseBtn = duplicateModal.querySelector(".close-btn");
      const closeDupBtn = document.getElementById("closeDuplicateModalBtn");

      dupCloseBtn?.addEventListener("click", closeDuplicateModal);
      closeDupBtn?.addEventListener("click", closeDuplicateModal);
      duplicateModal.addEventListener("click", (e) => {
        if (e.target === duplicateModal) closeDuplicateModal();
      });
    }

    if (createSuccessModal) {
      const successCloseBtn = createSuccessModal.querySelector(".close-btn");
      const okBtn = document.getElementById("reviewCreateOkBtn");

      successCloseBtn?.addEventListener("click", () => {
        closeCreateSuccessModal();
        window.location.reload();
      });
      okBtn?.addEventListener("click", () => {
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
  })();

  // 리뷰 수정 모달
  (function () {
    const editModal = document.getElementById("editReviewModal");
    const updateSuccessModal = document.getElementById("reviewUpdateSuccessModal");
    if (!editModal) return;

    const closeBtn = editModal.querySelector(".close-btn");
    const form = document.getElementById("editReviewForm");
    const reviewIdInput = document.getElementById("editReviewId");
    const starsWrap = editModal.querySelector(".stars-input");
    const stars = Array.from(starsWrap.querySelectorAll(".star"));
    const ratingInput = editModal.querySelector('input[name="rating"]');
    const titleInput = editModal.querySelector('input[name="title"]');
    const contentInput = editModal.querySelector('textarea[name="content"]');
    const titleCharCount = document.getElementById("editTitleCharCount");
    const contentCharCount = document.getElementById("editContentCharCount");

    const updateCharCount = (inputElement, countElement, maxLength) => {
      const currentLength = inputElement.value.length;
      countElement.textContent = currentLength;
      countElement.parentElement.className = "char-count";
      if (currentLength > maxLength * 0.9) {
        countElement.parentElement.classList.add("warning");
      }
      if (currentLength >= maxLength) {
        countElement.parentElement.classList.add("error");
      }
    };

    const closeModal = () => {
      editModal.style.display = "none";
    };

    stars.forEach((star) => {
      star.addEventListener("click", () => {
        const value = parseInt(star.dataset.value);
        ratingInput.value = value;
        stars.forEach((s, idx) => {
          if (idx < value) {
            s.classList.add("selected");
          } else {
            s.classList.remove("selected");
          }
        });
      });
    });

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

    closeBtn?.addEventListener("click", closeModal);
    editModal?.addEventListener("click", (e) => {
      if (e.target === editModal) closeModal();
    });

    document.querySelectorAll(".edit-btn").forEach((btn) => {
      btn.addEventListener("click", function () {
        const container = this.closest(".review-item-container");
        const reviewId = container.dataset.reviewId;
        const title = container.dataset.title;
        const content = container.dataset.content;
        const rating = container.dataset.rating;

        reviewIdInput.value = reviewId;
        titleInput.value = title;
        contentInput.value = content;
        ratingInput.value = rating;

        stars.forEach((s, idx) => {
          if (idx < rating) {
            s.classList.add("selected");
          } else {
            s.classList.remove("selected");
          }
        });

        updateCharCount(titleInput, titleCharCount, 50);
        updateCharCount(contentInput, contentCharCount, 500);

        editModal.style.setProperty("display", "flex", "important");
      });
    });

    if (form) {
      form.addEventListener("submit", async (e) => {
        e.preventDefault();

        if (Number(ratingInput.value) < 1) {
          alert("별점을 선택해주세요.");
          return;
        }

        const title = titleInput.value.trim();
        if (title.length === 0 || title.length > 50) {
          alert("리뷰 제목은 1자 이상 50자 이내로 입력해주세요.");
          return;
        }

        const content = contentInput.value.trim();
        if (content.length === 0 || content.length > 500) {
          alert("리뷰 내용은 1자 이상 500자 이내로 입력해주세요.");
          return;
        }

        const eventId = document.querySelector('input[name="eventId"]').value;
        const reviewId = reviewIdInput.value;
        const formData = new FormData(form);

        try {
          const response = await window.axiosInstance.put(`/events/${eventId}/reviews/${reviewId}`, formData, {
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
          });

          if (response.data.success) {
            closeModal();
            openUpdateSuccessModal();
          } else {
            alert(response.data.message || "리뷰 수정에 실패했습니다.");
          }
        } catch (error) {
          console.error("리뷰 수정 오류:", error);
          alert(error.response?.data?.message || "리뷰 수정 중 오류가 발생했습니다.");
        }
      });
    }

    const openUpdateSuccessModal = () => {
      updateSuccessModal.style.setProperty("display", "flex", "important");
    };
    const closeUpdateSuccessModal = () => {
      updateSuccessModal.style.display = "none";
    };

    if (updateSuccessModal) {
      const successCloseBtn = updateSuccessModal.querySelector(".close-btn");
      const okBtn = document.getElementById("reviewUpdateOkBtn");

      successCloseBtn?.addEventListener("click", () => {
        closeUpdateSuccessModal();
        window.location.reload();
      });
      okBtn?.addEventListener("click", () => {
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
  })();

  // 리뷰 삭제 모달
  (function () {
    const deleteConfirmModal = document.getElementById("reviewDeleteConfirmModal");
    const deleteSuccessModal = document.getElementById("reviewDeleteSuccessModal");
    if (!deleteConfirmModal) return;

    let currentReviewId = null;

    const openDeleteConfirmModal = () => {
      deleteConfirmModal.style.setProperty("display", "flex", "important");
    };
    const closeDeleteConfirmModal = () => {
      deleteConfirmModal.style.display = "none";
    };

    const openDeleteSuccessModal = () => {
      deleteSuccessModal.style.setProperty("display", "flex", "important");
    };
    const closeDeleteSuccessModal = () => {
      deleteSuccessModal.style.display = "none";
    };

    document.querySelectorAll(".delete-btn").forEach((btn) => {
      btn.addEventListener("click", function () {
        const container = this.closest(".review-item-container");
        currentReviewId = container.dataset.reviewId;
        openDeleteConfirmModal();
      });
    });

    if (deleteConfirmModal) {
      const confirmCloseBtn = deleteConfirmModal.querySelector(".close-btn");
      const confirmBtn = document.getElementById("reviewDeleteConfirmBtn");
      const cancelBtn = document.getElementById("reviewDeleteCancelBtn");

      confirmCloseBtn?.addEventListener("click", closeDeleteConfirmModal);
      cancelBtn?.addEventListener("click", closeDeleteConfirmModal);
      deleteConfirmModal.addEventListener("click", (e) => {
        if (e.target === deleteConfirmModal) closeDeleteConfirmModal();
      });

      confirmBtn?.addEventListener("click", async () => {
        if (!currentReviewId) return;

        const eventId = document.querySelector('input[name="eventId"]').value;

        try {
          const response = await window.axiosInstance.delete(`/events/${eventId}/reviews/${currentReviewId}`);

          if (response.data.success) {
            closeDeleteConfirmModal();
            openDeleteSuccessModal();
          } else {
            alert(response.data.message || "리뷰 삭제에 실패했습니다.");
          }
        } catch (error) {
          console.error("리뷰 삭제 오류:", error);
          alert(error.response?.data?.message || "리뷰 삭제 중 오류가 발생했습니다.");
        }
      });
    }

    if (deleteSuccessModal) {
      const successCloseBtn = deleteSuccessModal.querySelector(".close-btn");
      const okBtn = document.getElementById("reviewDeleteOkBtn");

      if (successCloseBtn) {
        successCloseBtn.addEventListener("click", () => {
          closeDeleteSuccessModal();
          window.location.reload();
        });
      }
      if (okBtn) {
        okBtn.addEventListener("click", () => {
          closeDeleteSuccessModal();
          window.location.reload();
        });
      }
      deleteSuccessModal.addEventListener("click", (e) => {
        if (e.target === deleteSuccessModal) {
          closeDeleteSuccessModal();
          window.location.reload();
        }
      });
    }
  })();

  // 2. 예약 버튼 기능 초기화
  initializeReserveButton();

  // 3. 카카오맵 기능 초기화
  initializeKakaoMap();
});

/**
 * 예약 버튼의 상태를 날짜에 따라 동적으로 설정하는 함수
 */
function initializeReserveButton() {
  const reserveBtn = document.getElementById("reserve-btn");
  const eventId = reserveBtn.dataset.eventId;
  if (!reserveBtn) {
    console.error("예약 버튼(id='reserve-btn')을 찾을 수 없습니다.");
    return;
  }

  const eventDateStr = reserveBtn.dataset.eventDate;
  if (!eventDateStr) {
    reserveBtn.textContent = "날짜 정보 없음";
    reserveBtn.disabled = true;
    reserveBtn.style.backgroundColor = "#F0F0F0";
    return;
  }

  const now = new Date();
  now.setHours(0, 0, 0, 0);

  const eventDate = new Date(eventDateStr);
  eventDate.setHours(0, 0, 0, 0);

  const ticketingOpenDate = new Date(eventDate);
  ticketingOpenDate.setDate(ticketingOpenDate.getDate() - 30);
  ticketingOpenDate.setHours(18, 0, 0, 0);

  let countdownInterval;

  function updateButtonState() {
    const currentTime = new Date();

    // 1. 예매가 종료된 경우
    if (now.getTime() >= eventDate.getTime()) {
      reserveBtn.textContent = "예매가 종료되었습니다";
      reserveBtn.disabled = true;
      reserveBtn.style.backgroundColor = "#F0F0F0";
      if (countdownInterval) clearInterval(countdownInterval);
      return;
    }

    // 2. 예매가 가능한 경우
    if (currentTime.getTime() >= ticketingOpenDate.getTime()) {
      reserveBtn.textContent = "예매하기";
      reserveBtn.disabled = false;
      reserveBtn.style.backgroundColor = "#f9d849";

      reserveBtn.onclick = function (e) {
        if (e) e.preventDefault();
        onModal(eventId);
      };

      if (countdownInterval) clearInterval(countdownInterval);
      return;
    }

    const countdownStartDate = new Date(ticketingOpenDate);
    countdownStartDate.setDate(countdownStartDate.getDate() - 1);

    // 3. 카운트다운이 필요한 경우
    if (currentTime.getTime() >= countdownStartDate.getTime()) {
      reserveBtn.disabled = true;
      reserveBtn.style.backgroundColor = "#F0F0F0";
      countdownInterval = setInterval(() => {
        const timeLeft = ticketingOpenDate.getTime() - new Date().getTime();
        if (timeLeft <= 0) {
          clearInterval(countdownInterval);
          updateButtonState();
          return;
        }
        const hours = Math.floor(timeLeft / (1000 * 60 * 60));
        const minutes = Math.floor((timeLeft % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((timeLeft % (1000 * 60)) / 1000);
        reserveBtn.textContent = `예매 오픈 까지 ${String(hours).padStart(2, "0")}시간 ${String(minutes).padStart(2, "0")}분 ${String(seconds).padStart(2, "0")}초`;
      }, 1000);
    } else {
      // 4. 아직 오픈 일자가 많이 남은 경우
      const openDate = ticketingOpenDate.toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
      });
      reserveBtn.textContent = `${openDate} 18:00 오픈예정`;
      reserveBtn.disabled = true;
      reserveBtn.style.backgroundColor = "#F0F0F0";
    }
  }

  // 함수 최초 실행
  updateButtonState();
}

/**
 * eventAddress 변수를 기반으로 카카오맵을 생성하고 마커를 표시하는 함수
 */
function initializeKakaoMap() {
  if (typeof eventAddress === "undefined" || !eventAddress) {
    console.error("이벤트 주소(eventAddress)를 찾을 수 없습니다. HTML 파일에 inline script가 있는지 확인하세요.");
    return;
  }

  const mapContainer = document.getElementById("map");
  if (!mapContainer) {
    console.error("지도를 표시할 컨테이너(id='map')를 찾을 수 없습니다.");
    return;
  }

  const mapOption = {
    center: new kakao.maps.LatLng(37.566826, 126.9786567), // 기본 중심: 서울 시청
    level: 5,
  };
  const map = new kakao.maps.Map(mapContainer, mapOption);
  const ps = new kakao.maps.services.Places();
  const fallbackAddress = "서울특별시 강남구 도곡로 112";

  function searchAndDisplay(address) {
    ps.keywordSearch(address, function (data, status) {
      if (status === kakao.maps.services.Status.OK && data.length > 0) {
        const firstResult = data[0];
        const coords = new kakao.maps.LatLng(firstResult.y, firstResult.x);

        const marker = new kakao.maps.Marker({
          map: map,
          position: coords,
        });

        const label = address === fallbackAddress ? "테크브루 아카데미" : firstResult.place_name;
        const infowindow = new kakao.maps.InfoWindow({
          content: `<div style="padding:5px;font-size:12px;width:max-content;">${label}</div>`,
        });
        infowindow.open(map, marker);

        map.setCenter(coords);
      } else {
        console.warn(`'${address}'에 대한 검색 결과가 없습니다.`);
        if (address !== fallbackAddress) {
          searchAndDisplay(fallbackAddress);
        } else {
          mapContainer.innerHTML = '<div style="display:flex; align-items:center; justify-content:center; height:100%;">장소 정보를 찾을 수 없습니다.</div>';
        }
      }
    });
  }

  searchAndDisplay(eventAddress);
}
