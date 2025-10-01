document.addEventListener("DOMContentLoaded", async () => {
  const statusPanel = document.getElementById("statusPanel");
  const orderSection = document.getElementById("orderSection");
  const orderHistory = document.getElementById("orderHistory");
  const orderEmpty = document.getElementById("orderEmpty");
  const guideSection = document.getElementById("guideSection");
  const loginSection = document.getElementById("loginSection");
  const trackingForm = document.getElementById("trackingForm");
  const trackingKeyInput = trackingForm?.querySelector('input[name="t_key"]');
  const trackingCodeInput = trackingForm?.querySelector('input[name="t_code"]');
  const trackingInvoiceInput = trackingForm?.querySelector('input[name="t_invoice"]');

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
  } catch (error) {
    console.error("주문 목록 조회 실패", error);

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
                <div class="item-cover">
                  ${coverHtml}
                </div>
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
            <div class="order-actions">
              ${trackingButton}
            </div>
          </div>

          <div class="order-card__body">
            <ul class="order-item-list">
              ${itemsHtml}
            </ul>
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
    if (!orders || orders.length === 0) {
      guideSection.style.display = "block";
    } else {
      guideSection.style.display = "none";
    }
  }

  function showLoginRequired() {
    if (loginSection) loginSection.style.display = "block";
    if (statusPanel) statusPanel.style.display = "none";
    if (orderSection) orderSection.style.display = "none";
    if (guideSection) guideSection.style.display = "none";
  }

  function formatPrice(price) {
    if (price === null || price === undefined) return "0원";
    return `${Number(price).toLocaleString()}원`;
  }

  function wrapWithLink(content, link, className) {
    if (!link) return content;
    return `<a href="${link}" class="${className}">${content}</a>`;
  }

  const reviewModal = (() => {
    const modal = document.getElementById("reviewModal");
    if (!modal) {
      return {
        open: () => {},
        close: () => {},
      };
    }

    const closeBtn = modal.querySelector(".close-btn");
    const starsWrap = document.getElementById("reviewStars");
    const stars = starsWrap ? Array.from(starsWrap.querySelectorAll(".star")) : [];
    const ratingInput = document.getElementById("reviewRating");
    const orderIdInput = document.getElementById("reviewOrderId");
    const bookIdInput = document.getElementById("reviewBookId");
    const reviewContent = document.getElementById("reviewContent");
    const targetLabel = document.getElementById("reviewTargetLabel");
    const form = document.getElementById("reviewForm");

    const reset = () => {
      if (ratingInput) {
        ratingInput.value = 0;
      }
      stars.forEach((star) => star.classList.remove("selected"));
      if (reviewContent) {
        reviewContent.value = "";
      }
    };

    const open = (orderId, bookId, bookTitle) => {
      if (orderIdInput) {
        orderIdInput.value = orderId ?? "";
      }
      if (bookIdInput) {
        bookIdInput.value = bookId ?? "";
      }
      reset();
      if (targetLabel) {
        targetLabel.textContent = bookTitle ? `상품: ${bookTitle}` : "";
        targetLabel.style.display = bookTitle ? "block" : "none";
      }
      modal.style.display = "flex";
    };

    const close = () => {
      modal.style.display = "none";
    };

    if (closeBtn) {
      closeBtn.addEventListener("click", close);
    }

    modal.addEventListener("click", (event) => {
      if (event.target === modal) {
        close();
      }
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        close();
      }
    });

    if (starsWrap && ratingInput) {
      starsWrap.addEventListener("click", (event) => {
        const star = event.target.closest(".star");
        if (!star) return;

        const value = Number(star.dataset.value);
        ratingInput.value = value;
        stars.forEach((s) => {
          const shouldSelect = Number(s.dataset.value) <= value;
          s.classList.toggle("selected", shouldSelect);
        });
      });
    }

    if (form && ratingInput) {
      form.addEventListener("submit", (event) => {
        if (Number(ratingInput.value) < 1) {
          event.preventDefault();
          alert("별점을 선택해주세요.");
          return;
        }

        event.preventDefault();
        alert("리뷰 작성이 준비 중입니다.");
        close();
      });
    }

    return { open, close };
  })();

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

  function handleTrackingButtonClick(event) {
    const target = event.target;
    if (!trackingForm || !trackingKeyInput || !trackingCodeInput || !trackingInvoiceInput) return;

    if (target && target.classList.contains("btn-tracking")) {
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

      openTrackingPopup(event);
    }
  }

  function handleReviewButtonClick(event) {
    const target = event.target;
    if (!(target instanceof HTMLElement)) return;

    if (target.classList.contains("btn-review")) {
      const orderId = target.dataset.orderId;
      const bookId = target.dataset.bookId;
      const bookTitle = target.dataset.bookTitle;

      reviewModal.open(orderId, bookId, bookTitle);
    }
  }

  function openTrackingPopup(event) {
    if (!trackingForm) return;
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

    setTimeout(() => {
      trackingForm.submit();
    }, 10);
  }

  orderHistory?.addEventListener("click", handleTrackingButtonClick);
  orderHistory?.addEventListener("click", handleReviewButtonClick);
  trackingForm?.addEventListener("submit", openTrackingPopup);
});
