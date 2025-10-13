// 알림 모달 함수
function showOrderAlertModal(message, title = "알림") {
  const modal = document.getElementById("orderAlertModal");
  const titleElement = document.getElementById("orderAlertModalTitle");
  const messageElement = document.getElementById("orderAlertModalMessage");

  if (titleElement) titleElement.textContent = title;
  if (messageElement) messageElement.textContent = message;

  if (modal) {
    modal.style.display = "flex";
  }
}

window.closeOrderAlertModal = function () {
  const modal = document.getElementById("orderAlertModal");
  if (modal) {
    modal.style.display = "none";
  }
};

// 리뷰 모달 초기화 (전역, 한 번만)
function initReviewModal() {
  if (window._reviewModalInitialized) return;
  window._reviewModalInitialized = true;

  const open = async (orderId, bookId, bookTitle) => {
    try {
      const checkResponse = await window.axiosInstance.get(`/books/${bookId}/reviews/check`);
      const checkData = checkResponse.data;

      if (checkData.success && checkData.hasReview) {
        showOrderAlertModal("이미 해당 도서에 리뷰를 작성하셨습니다.", "중복 리뷰");
        return;
      }
    } catch (error) {
      if (error.response && error.response.status === 401) {
        alert("로그인이 필요합니다.");
        return;
      }
    }

    // 기존에 생성된 동적 모달이 있다면 제거 (메모리 누수 방지)
    const existingModal = document.getElementById("reviewModalNew");
    if (existingModal) {
      existingModal.remove();
    }

    // 모달 완전히 새로 생성
    const newModal = document.createElement("div");
    newModal.id = "reviewModalNew";
    newModal.style.cssText = `
      display: flex !important;
      position: fixed !important;
      top: 0 !important;
      left: 0 !important;
      width: 100vw !important;
      height: 100vh !important;
      background-color: rgba(0, 0, 0, 0.8) !important;
      justify-content: center !important;
      align-items: center !important;
      z-index: 99999 !important;
      font-family: Arial, sans-serif !important;
    `;

    newModal.innerHTML = `
      <div style="
        background: white;
        padding: 30px;
        border-radius: 8px;
        width: 90%;
        max-width: 500px;
        position: relative;
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
      ">
        <button class="close-modal-btn" style="
          position: absolute;
          top: 15px;
          right: 20px;
          width: 32px;
          height: 32px;
          border: none;
          background: none;
          font-size: 24px;
          cursor: pointer;
        ">&times;</button>
        <h2 style="margin: 0 0 20px 0; font-size: 20px;">리뷰 작성</h2>
        <p style="margin: 0 0 15px 0; color: #666;">상품: ${bookTitle}</p>
        <form id="reviewFormNew">
          <input type="hidden" id="reviewOrderIdNew" name="orderId" value="${orderId}" />
          <input type="hidden" id="reviewBookIdNew" name="bookId" value="${bookId}" />
          
          <div style="margin-bottom: 15px;">
            <label style="display: block; margin-bottom: 5px; font-weight: bold;">별점</label>
            <div id="reviewStarsNew" style="display: flex; gap: 5px;">
              <span class="star" data-value="1" style="font-size: 24px; cursor: pointer; color: #ddd;">★</span>
              <span class="star" data-value="2" style="font-size: 24px; cursor: pointer; color: #ddd;">★</span>
              <span class="star" data-value="3" style="font-size: 24px; cursor: pointer; color: #ddd;">★</span>
              <span class="star" data-value="4" style="font-size: 24px; cursor: pointer; color: #ddd;">★</span>
              <span class="star" data-value="5" style="font-size: 24px; cursor: pointer; color: #ddd;">★</span>
            </div>
            <input type="hidden" id="reviewRatingNew" name="rating" value="0" required />
          </div>
          
          <div style="margin-bottom: 15px;">
            <label style="display: block; margin-bottom: 5px; font-weight: bold;">리뷰 제목</label>
            <input type="text" id="reviewTitleNew" name="title" placeholder="리뷰 제목을 입력해주세요." maxlength="50" required 
              style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box;" />
            <div style="text-align: right; font-size: 12px; color: #666;">
              <span id="reviewTitleCharCountNew">0</span> / 50
            </div>
          </div>
          
          <div style="margin-bottom: 20px;">
            <label style="display: block; margin-bottom: 5px; font-weight: bold;">리뷰 내용</label>
            <textarea id="reviewContentNew" name="content" rows="6" placeholder="리뷰 내용을 입력해주세요." maxlength="500" required
              style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; resize: vertical;"></textarea>
            <div style="text-align: right; font-size: 12px; color: #666;">
              <span id="reviewContentCharCountNew">0</span> / 500
            </div>
          </div>
          
          <button type="submit" style="
            width: 100%;
            padding: 12px;
            background: rgb(181, 209, 115);
            color: white;
            border: none;
            border-radius: 4px;
            font-size: 16px;
            cursor: pointer;
            font-weight: bold;
          ">등록하기</button>
        </form>
      </div>
    `;

    // 닫기 버튼 이벤트
    const closeBtn = newModal.querySelector(".close-modal-btn");
    closeBtn.addEventListener("click", () => {
      newModal.remove();
    });

    // 모달 외부 클릭 시 닫기
    newModal.addEventListener("click", (e) => {
      if (e.target === newModal) {
        newModal.remove();
      }
    });

    // 별점 이벤트
    newModal.querySelectorAll(".star").forEach((star, index) => {
      star.addEventListener("click", () => {
        const rating = index + 1;
        newModal.querySelector("#reviewRatingNew").value = rating;
        newModal.querySelectorAll(".star").forEach((s, i) => {
          s.style.color = i < rating ? "rgb(181, 209, 115)" : "#ddd";
        });
      });
    });

    // 글자수 카운트
    const titleInput = newModal.querySelector("#reviewTitleNew");
    const titleCount = newModal.querySelector("#reviewTitleCharCountNew");
    const contentInput = newModal.querySelector("#reviewContentNew");
    const contentCount = newModal.querySelector("#reviewContentCharCountNew");

    titleInput.addEventListener("input", () => {
      titleCount.textContent = titleInput.value.length;
    });

    contentInput.addEventListener("input", () => {
      contentCount.textContent = contentInput.value.length;
    });

    // 폼 제출
    const formElement = newModal.querySelector("#reviewFormNew");
    formElement.addEventListener("submit", async (e) => {
      e.preventDefault();

      const rating = parseInt(newModal.querySelector("#reviewRatingNew").value);
      if (rating < 1) {
        alert("별점을 선택해주세요.");
        return;
      }

      const formData = new FormData(e.target);

      try {
        const response = await window.axiosInstance.post(`/books/${bookId}/reviews`, formData, {
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
        });

        if (response.data.success) {
          newModal.remove();
          showOrderAlertModal("리뷰가 작성되었습니다.", "작성 완료");
          await loadOrderData();
        } else {
          alert(response.data.message || "리뷰 작성에 실패했습니다.");
        }
      } catch (error) {
        alert("리뷰 작성 중 오류가 발생했습니다.");
      }
    });

    document.body.appendChild(newModal);
  };

  const close = () => {
    const modal = document.getElementById("reviewModalNew");
    if (modal) {
      modal.remove();
    }
  };

  // ESC 키 이벤트 - 중복 등록 방지를 위한 플래그 확인
  if (!window._reviewModalEscListenerAdded) {
    window._reviewModalEscListenerAdded = true;
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        const reviewModalNew = document.getElementById("reviewModalNew");
        if (reviewModalNew) {
          reviewModalNew.remove();
        }
      }
    });
  }

  window.reviewModal = { open, close };
}

// 이벤트 핸들러 (전역)
function handleTrackingButtonClick(event) {
  const target = event.target;
  if (!target.classList.contains("btn-tracking")) return;

  const trackingForm = document.getElementById("trackingForm");
  const trackingKeyInput = trackingForm?.querySelector('input[name="t_key"]');
  const trackingCodeInput = trackingForm?.querySelector('input[name="t_code"]');
  const trackingInvoiceInput = trackingForm?.querySelector('input[name="t_invoice"]');

  if (!trackingForm || !trackingKeyInput || !trackingCodeInput || !trackingInvoiceInput) return;

  const trackingNumber = target.getAttribute("data-tracking");
  if (!trackingKeyInput.value) {
    alert("배송 조회 API 키가 설정되지 않았습니다.");
    return;
  }
  if (!trackingNumber) {
    alert("운송장 번호가 존재하지 않습니다.");
    return;
  }

  trackingCodeInput.value = "04";
  trackingInvoiceInput.value = trackingNumber;

  const width = 520;
  const height = 720;
  const left = window.screenX + (window.outerWidth - width) / 2;
  const top = window.screenY + (window.outerHeight - height) / 2;

  const popup = window.open("", "trackingPopup", `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`);

  if (!popup || popup.closed) {
    alert("팝업이 차단되었습니다. 팝업 차단을 해제하고 다시 시도해주세요.");
    event.preventDefault();
    return;
  }

  setTimeout(() => trackingForm.submit(), 10);
}

function handleReviewButtonClick(event) {
  const target = event.target;
  if (!(target instanceof HTMLElement)) return;
  if (!target.classList.contains("btn-review")) return;

  const orderId = target.dataset.orderId;
  const bookId = target.dataset.bookId;
  const bookTitle = target.dataset.bookTitle;

  if (window.reviewModal && window.reviewModal.open) {
    window.reviewModal.open(orderId, bookId, bookTitle);
  }
}

// 데이터 로드 함수
async function loadOrderData() {
  const statusPanel = document.getElementById("statusPanel");
  const orderSection = document.getElementById("orderSection");
  const orderHistory = document.getElementById("orderHistory");
  const orderEmpty = document.getElementById("orderEmpty");
  const guideSection = document.getElementById("guideSection");
  const loginSection = document.getElementById("loginSection");

  const statusCounts = {
    ready: document.querySelector(".status-count[data-type='ready']"),
    shipping: document.querySelector(".status-count[data-type='shipping']"),
    completed: document.querySelector(".status-count[data-type='completed']"),
    return: document.querySelector(".status-count[data-type='return']"),
  };

  const accessToken = localStorage.getItem("accessToken");
  if (!accessToken) {
    showLoginRequired();
    return;
  }

  if (typeof window.validateUser === "function") {
    try {
      await window.validateUser();
    } catch (err) {
      showLoginRequired();
      return;
    }
  }

  function formatPrice(price) {
    if (price === null || price === undefined) return "0원";
    return `${Number(price).toLocaleString()}원`;
  }

  function wrapWithLink(content, link, className) {
    if (!link) return content;
    return `<a href="${link}" class="${className}">${content}</a>`;
  }

  function createSecondaryButtons(statusCode) {
    if (statusCode === 1) {
      return `
        <button type="button" class="btn-secondary">취소신청</button>
        <button type="button" class="btn-secondary">배송지 변경</button>
      `;
    }
    if (statusCode === 3 || statusCode === 4) {
      return `
        <button type="button" class="btn-secondary">환불신청</button>
        <button type="button" class="btn-secondary">교환신청</button>
      `;
    }
    return "";
  }

  function renderStatus(summary) {
    if (!summary) return;
    if (statusCounts.ready) statusCounts.ready.textContent = summary.ready ?? 0;
    if (statusCounts.shipping) statusCounts.shipping.textContent = summary.shipping ?? 0;
    if (statusCounts.completed) statusCounts.completed.textContent = summary.completed ?? 0;
    if (statusCounts.return) statusCounts.return.textContent = summary.returned ?? 0;
    if (statusPanel) statusPanel.style.display = "block";
  }

  function renderOrders(orders) {
    if (!orderSection || !orderHistory || !orderEmpty) return;
    orderSection.style.display = "block";

    if (!orders || orders.length === 0) {
      orderHistory.innerHTML = "";
      orderEmpty.style.display = "block";
      return;
    }

    orderEmpty.style.display = "none";

    const html = orders
      .map((order) => {
        const statusCode = Number(order.orderStatus);
        const canWriteReview = statusCode === 3;
        const itemsHtml = (order.items || [])
          .map((item) => {
            const bookLink = item.bookId ? `/books/${item.bookId}` : null;
            const safeTitle = item.title ?? "도서 정보 없음";
            const coverContent = item.coverImage ? `<img src="${item.coverImage}" alt="${safeTitle}" />` : '<div class="thumb-placeholder"><span>이미지 없음</span></div>';
            const coverHtml = wrapWithLink(coverContent, bookLink, "book-link book-link--cover");
            const titleHtml = wrapWithLink(safeTitle, bookLink, "book-link book-link--text");
            const authorText = item.author ?? "";
            const metaText = `${item.quantity ?? 0}권 · ${formatPrice(item.price)}`;
            const metaHtml = wrapWithLink(metaText, bookLink, "book-link book-link--text");
            const authorRow = authorText ? `<div class="item-meta">${wrapWithLink(authorText, bookLink, "book-link book-link--text")}</div>` : "";
            const reviewButton = canWriteReview ? `<div class="item-actions"><button type="button" class="btn-review" data-order-id="${order.orderId ?? ""}" data-book-id="${item.bookId ?? ""}" data-book-title="${safeTitle}">리뷰작성</button></div>` : "";

            return `
              <li class="order-item">
                <div class="item-cover">${coverHtml}</div>
                <div class="item-info">
                  <div class="item-title">${titleHtml}</div>
                  ${authorRow}
                  <div class="item-meta">${metaHtml}</div>
                </div>
                ${reviewButton}
              </li>
            `;
          })
          .join("");

        const trackingButton = statusCode === 4 && order.trackingNumber ? `<button type="button" class="btn-secondary btn-tracking" data-tracking="${order.trackingNumber}">배송조회</button>` : "";
        const secondaryButtons = createSecondaryButtons(statusCode);

        return `
        <article class="order-card" data-status="${order.orderStatus ?? ""}">
          <div class="order-card__header">
            <div class="order-meta">
              <div class="meta-row">
                <span class="meta-label">주문일자</span>
                <span class="meta-value">${order.orderDate ?? "-"}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">주문상태</span>
                <span class="meta-value status-text">${order.statusLabel ?? "상태 미정"}</span>
              </div>
            </div>
            <div class="order-actions">${trackingButton}</div>
          </div>
          <div class="order-card__body">
            <ul class="order-item-list">${itemsHtml}</ul>
          </div>
          <footer class="order-card__footer">
            <div class="payment-info-row">
              <span class="meta-label">결제금액</span>
              <span class="meta-value price">${formatPrice(order.totalPrice)}</span>
            </div>
            <div class="payment-info-row">
              <span class="meta-label">배송비</span>
              <span class="meta-value">${formatPrice(order.shippingFee)}</span>
            </div>
            <div class="order-actions-secondary">${secondaryButtons}</div>
          </footer>
        </article>
      `;
      })
      .join("");

    orderHistory.innerHTML = html;
  }

  function toggleGuide(orders) {
    if (!guideSection) return;
    guideSection.style.display = !orders || orders.length === 0 ? "block" : "none";
  }

  function updateMypageProfileName(userName) {
    if (!userName) return;
    const nameElement = document.querySelector(".mypage-profile__name strong");
    if (nameElement) nameElement.textContent = userName;
  }

  function showLoginRequired() {
    if (loginSection) loginSection.style.display = "block";
    if (statusPanel) statusPanel.style.display = "none";
    if (orderSection) orderSection.style.display = "none";
    if (guideSection) guideSection.style.display = "none";
  }

  try {
    const response = await window.axiosInstance.get("/order/orderList/api");
    const data = response.data;

    if (!data?.success) {
      showLoginRequired();
      return;
    }

    renderStatus(data.statusSummary);
    renderOrders(data.orders);
    toggleGuide(data.orders);
    updateMypageProfileName(data.userName);
  } catch (error) {
    const status = error.response?.status;
    if (status === 401 || status === 403) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      alert("세션이 만료되었습니다. 다시 로그인해주세요.");
      window.location.href = "/user/login";
      return;
    }
    alert("주문 목록을 불러오는 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
  }
}

// 이벤트 리스너 등록 (한 번만)
function initEventListeners() {
  if (window._orderListInitialized) return;
  window._orderListInitialized = true;

  const orderHistory = document.getElementById("orderHistory");
  const trackingForm = document.getElementById("trackingForm");

  if (orderHistory) {
    orderHistory.addEventListener("click", handleTrackingButtonClick);
    orderHistory.addEventListener("click", handleReviewButtonClick);
  }

  if (trackingForm) {
    trackingForm.addEventListener("submit", (e) => e.preventDefault());
  }
}

// 알림 모달 초기화
function initAlertModal() {
  let alertModal = document.getElementById("orderAlertModal");

  if (!alertModal) {
    alertModal = document.createElement("div");
    alertModal.id = "orderAlertModal";
    alertModal.className = "modal-overlay review-modal";
    alertModal.style.display = "none";

    alertModal.innerHTML = `
      <div class="modal-content alert-modal-content">
        <div class="modal-header-simple">
          <h3 id="orderAlertModalTitle">알림</h3>
          <button class="close-btn" onclick="window.closeOrderAlertModal()">&times;</button>
        </div>
        <div class="modal-body-simple">
          <p id="orderAlertModalMessage"></p>
        </div>
        <div class="modal-buttons-simple">
          <button type="button" class="submit-btn" onclick="window.closeOrderAlertModal()">확인</button>
        </div>
      </div>
    `;

    document.body.appendChild(alertModal);
  }
}

// 주문배송조회 초기화 함수
window.initOrderList = async function () {
  initAlertModal();
  initReviewModal();
  await loadOrderData();
  initEventListeners();
};
