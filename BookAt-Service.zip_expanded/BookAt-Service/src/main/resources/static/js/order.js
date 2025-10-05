// 주문 상품 정보를 저장할 변수
let orderItems = [];

// 서버에서 전달받은 도서 정보를 초기화하는 함수
function initializeOrderData() {
  // Thymeleaf에서 전달받은 데이터가 이미 window 객체에 설정되어 있는지 확인
  if (typeof window.bookData === "undefined") {
    window.bookData = {
      bookId: null,
      title: null,
      author: null,
      price: 0,
      imageUrl: null,
      coverImage: null,
    };
  }

  if (typeof window.quantity === "undefined") {
    window.quantity = 1;
  }

  if (typeof window.isDirectOrder === "undefined") {
    window.isDirectOrder = false;
  }
}

// 전화번호 포맷팅 함수
function formatPhoneNumber(phone) {
  if (!phone) return "";

  // 숫자만 추출
  const numbers = phone.replace(/\D/g, "");

  // 11자리인 경우 (010-1234-5678)
  if (numbers.length === 11) {
    return numbers.replace(/(\d{3})(\d{4})(\d{4})/, "$1-$2-$3");
  }
  // 10자리인 경우 (02-1234-5678)
  else if (numbers.length === 10) {
    return numbers.replace(/(\d{2})(\d{4})(\d{4})/, "$1-$2-$3");
  }
  // 9자리인 경우 (02-123-4567)
  else if (numbers.length === 9) {
    return numbers.replace(/(\d{2})(\d{3})(\d{4})/, "$1-$2-$3");
  }

  // 포맷팅할 수 없는 경우 원본 반환
  return phone;
}

// 페이지의 전화번호를 포맷팅하는 함수
function formatPhoneNumbers() {
  // 전화번호가 표시되는 요소들을 찾아서 포맷팅
  const phoneElements = document.querySelectorAll(".phone-number");

  phoneElements.forEach((element) => {
    const text = element.textContent;
    if (text && text.trim()) {
      element.textContent = formatPhoneNumber(text);
    }
  });
}

// 페이지 로드 시 로그인 검증 및 주문 상품 정보를 가져와서 표시
document.addEventListener("DOMContentLoaded", async function () {
  // 데이터 초기화
  initializeOrderData();

  // 로그인 검증
  checkLoginStatus();

  // 전화번호 포맷팅 적용
  formatPhoneNumbers();

  // 세션스토리지에서 주문 상품 정보를 가져옴 (장바구니 또는 바로구매)
  const itemsData = sessionStorage.getItem("orderItems");
  const directOrderData = sessionStorage.getItem("directOrderItem");

  if (itemsData) {
    // 장바구니에서 온 경우
    try {
      orderItems = JSON.parse(itemsData);
      renderOrderItems();
      updateOrderSummary();
      // 사용 후 세션스토리지에서 제거
      sessionStorage.removeItem("orderItems");
    } catch (e) {
      console.error("주문 상품 정보를 파싱하는 중 오류가 발생했습니다:", e);
      showOrderResultModal("주문 상품 정보를 불러올 수 없습니다.", false);
      return;
    }
  } else if (directOrderData) {
    // 바로구매에서 온 경우
    try {
      // 세션 스토리지에서 바로구매 데이터 확인
      const directOrderDataFromStorage = sessionStorage.getItem("directOrderData");
      if (!directOrderDataFromStorage) {
        showOrderResultModal("바로구매 데이터를 불러올 수 없습니다.", false);
        return;
      }

      const orderData = JSON.parse(directOrderDataFromStorage);
      if (!orderData.success || !orderData.book) {
        showOrderResultModal("바로구매 데이터를 불러올 수 없습니다.", false);
        return;
      }

      const book = orderData.book;
      const quantity = orderData.quantity;

      orderItems = [
        {
          bookId: book.bookId,
          title: book.title,
          author: book.author,
          price: book.price,
          quantity: quantity,
          coverImage: book.imageUrl,
          isDirectOrder: true,
        },
      ];

      renderOrderItems();
      updateOrderSummary();

      // 사용 후 세션스토리지에서 제거
      sessionStorage.removeItem("directOrderItem");
      sessionStorage.removeItem("directOrderData");
    } catch (e) {
      console.error("바로구매 상품 정보를 파싱하는 중 오류가 발생했습니다:", e);
      showOrderResultModal("주문 상품 정보를 불러올 수 없습니다.", false);
      return;
    }
  } else {
    showOrderResultModal("주문할 상품이 없습니다.", false);
    return;
  }
});

// 로그인 상태 확인 함수 (서버에서 이미 검증했으므로 간단히 체크)
function checkLoginStatus() {
  const accessToken = localStorage.getItem("accessToken");

  if (!accessToken) {
    showOrderResultModal("로그인이 필요합니다.", false);
    return;
  }
}

// 주문 상품 목록을 렌더링
function renderOrderItems() {
  const container = document.getElementById("orderItems");
  container.innerHTML = "";

  orderItems.forEach((item) => {
    const itemElement = document.createElement("div");
    itemElement.className = "order-item";
    itemElement.innerHTML = `
                  <div class="item-image">
                      <img src="${item.coverImage}" alt="${item.title}">
                  </div>
                  <div class="item-details">
                      <h3>${item.title}</h3>
                      <p class="author">${item.author}</p>
                      <div class="item-price">
                          <span class="price">${item.price.toLocaleString()}원</span>
                          <span class="quantity">수량: ${item.quantity}개</span>
                      </div>
                  </div>
              `;
    container.appendChild(itemElement);
  });
}

// 주문 요약 업데이트
function updateOrderSummary() {
  const subtotal = orderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);

  // 배송비 정책: 15,000원 미만은 3,000원, 그 이상은 무료
  const shippingFee = subtotal > 0 && subtotal < 15000 ? 3000 : 0;
  const totalAmount = subtotal + shippingFee;
  const points = Math.floor(subtotal * 0.01); // 1% 적립

  document.getElementById("subtotal").textContent = subtotal.toLocaleString() + "원";
  document.getElementById("shippingFee").textContent = shippingFee === 0 ? "무료" : `${shippingFee.toLocaleString()}원`;
  document.getElementById("total").textContent = totalAmount.toLocaleString() + "원";
  document.getElementById("totalAmount").textContent = totalAmount.toLocaleString() + "원";
  document.getElementById("points").textContent = points.toLocaleString() + "P";
}

// 배송지 정보 입력 안내 모달 표시
function showDeliveryInfoModal() {
  const modal = document.createElement("div");
  modal.className = "delivery-info-modal";
  modal.style.cssText = `
    position: fixed;
    z-index: 2000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;
    box-sizing: border-box;
  `;

  modal.innerHTML = `
    <div style="
      background-color: #fff;
      border-radius: 8px;
      width: 90%;
      max-width: 400px;
      padding: 2rem;
      text-align: center;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
      transform: translateY(30px);
    ">
      <div style="
        font-size: 1.2rem;
        font-weight: bold;
        color: #333;
        margin-bottom: 1rem;
      ">배송지 정보를 입력해주세요</div>
      <div style="
        color: #666;
        margin-bottom: 2rem;
        line-height: 1.5;
      ">주문을 완료하기 위해서는<br>배송지 정보가 필요합니다.</div>
      <div style="display: flex; gap: 0.5rem; justify-content: center;">
        <button onclick="closeDeliveryInfoModal()" style="
          background-color: #6c757d;
          color: white;
          border: none;
          padding: 0.75rem 1.5rem;
          border-radius: 4px;
          cursor: pointer;
          font-size: 1rem;
        ">취소</button>
        <button onclick="closeDeliveryInfoModal(); openAddressModal();" style="
          background-color: #b5d173;
          color: white;
          border: none;
          padding: 0.75rem 1.5rem;
          border-radius: 4px;
          cursor: pointer;
          font-size: 1rem;
          font-weight: bold;
        ">배송지 입력하기</button>
      </div>
    </div>
  `;

  document.body.appendChild(modal);
}

// 배송지 정보 안내 모달 닫기
function closeDeliveryInfoModal() {
  const modal = document.querySelector(".delivery-info-modal");
  if (modal) {
    modal.remove();
  }
}

// 배송지 저장 성공 모달 표시
function showSaveSuccessModal() {
  const modal = document.createElement("div");
  modal.className = "save-success-modal";
  modal.style.cssText = `
    position: fixed;
    z-index: 2000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;
    box-sizing: border-box;
  `;

  modal.innerHTML = `
    <div style="
      background-color: #fff;
      border-radius: 8px;
      width: 90%;
      max-width: 350px;
      padding: 2rem;
      text-align: center;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
      transform: translateY(30px);
    ">
      <div style="
        font-size: 1.3rem;
        font-weight: bold;
        color: #b5d173;
        margin-bottom: 1rem;
      ">✓</div>
      <div style="
        font-size: 1.1rem;
        font-weight: bold;
        color: #333;
        margin-bottom: 0.5rem;
      ">저장 완료</div>
      <div style="
        color: #666;
        margin-bottom: 2rem;
        line-height: 1.5;
      ">배송지 정보가 성공적으로<br>저장되었습니다.</div>
      <button onclick="closeSaveSuccessModal()" style="
        background-color: #b5d173;
        color: white;
        border: none;
        padding: 0.75rem 2rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
        font-weight: bold;
        width: 100%;
      ">확인</button>
    </div>
  `;

  document.body.appendChild(modal);
}

// 배송지 저장 성공 모달 닫기
function closeSaveSuccessModal() {
  const modal = document.querySelector(".save-success-modal");
  if (modal) {
    modal.remove();
  }
}

// 주문 결과 모달 표시 (성공/실패)
function showOrderResultModal(message, isSuccess) {
  const modal = document.createElement("div");
  modal.className = "order-result-modal";
  modal.style.cssText = `
    position: fixed;
    z-index: 2000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 20px;
    box-sizing: border-box;
  `;

  const iconColor = isSuccess ? "#b5d173" : "#dc3545";
  const icon = isSuccess ? "✓" : "✕";
  const buttonColor = isSuccess ? "#b5d173" : "#dc3545";

  modal.innerHTML = `
    <div style="
      background-color: #fff;
      border-radius: 12px;
      width: 90%;
      max-width: 400px;
      padding: 2rem;
      text-align: center;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
      transform: translateY(0);
      animation: modalSlideIn 0.3s ease-out;
    ">
      <div style="
        font-size: 3rem;
        color: ${iconColor};
        margin-bottom: 1rem;
      ">${icon}</div>
      <div style="
        font-size: 1.2rem;
        font-weight: bold;
        color: #333;
        margin-bottom: 1rem;
        line-height: 1.4;
      ">${message}</div>
      <div style="display: flex; gap: 0.75rem; justify-content: center; margin-top: 2rem;">
        ${
          isSuccess
            ? `
          <button onclick="closeOrderResultModal(); window.location.href='/cart';" style="
            background-color: ${buttonColor};
            color: white;
            border: none;
            padding: 0.75rem 1.5rem;
            border-radius: 6px;
            cursor: pointer;
            font-size: 1rem;
            font-weight: bold;
            flex: 1;
            transition: background-color 0.2s;
          " onmouseover="this.style.backgroundColor='${isSuccess ? "#9bb85a" : "#c82333"}'" onmouseout="this.style.backgroundColor='${buttonColor}'">확인</button>
        `
            : `
          <button onclick="closeOrderResultModal()" style="
            background-color: #6c757d;
            color: white;
            border: none;
            padding: 0.75rem 1.5rem;
            border-radius: 6px;
            cursor: pointer;
            font-size: 1rem;
            margin-right: 0.5rem;
            transition: background-color 0.2s;
          " onmouseover="this.style.backgroundColor='#5a6268'" onmouseout="this.style.backgroundColor='#6c757d'">취소</button>
          ${
            message.includes("로그인")
              ? `
            <button onclick="closeOrderResultModal(); window.location.href='/user/login';" style="
              background-color: ${buttonColor};
              color: white;
              border: none;
              padding: 0.75rem 1.5rem;
              border-radius: 6px;
              cursor: pointer;
              font-size: 1rem;
              font-weight: bold;
              transition: background-color 0.2s;
            " onmouseover="this.style.backgroundColor='${isSuccess ? "#9bb85a" : "#c82333"}'" onmouseout="this.style.backgroundColor='${buttonColor}'">로그인</button>
          `
              : `
            <button onclick="closeOrderResultModal()" style="
              background-color: ${buttonColor};
              color: white;
              border: none;
              padding: 0.75rem 1.5rem;
              border-radius: 6px;
              cursor: pointer;
              font-size: 1rem;
              font-weight: bold;
              transition: background-color 0.2s;
            " onmouseover="this.style.backgroundColor='${isSuccess ? "#9bb85a" : "#c82333"}'" onmouseout="this.style.backgroundColor='${buttonColor}'">확인</button>
          `
          }
        `
        }
      </div>
    </div>
    <style>
      @keyframes modalSlideIn {
        from {
          opacity: 0;
          transform: translateY(-30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    </style>
  `;

  document.body.appendChild(modal);
}

// 주문 결과 모달 닫기
function closeOrderResultModal() {
  const modal = document.querySelector(".order-result-modal");
  if (modal) {
    modal.remove();
  }
}

// 주소 모달 관련 함수들

// 주소 모달 열기
function openAddressModal() {
  document.getElementById("addressModal").style.display = "block";
  // 현재 값으로 모달 필드 초기화 (배송지 정보가 없는 경우 빈 값으로 설정)
  const currentName = document.getElementById("display-name").textContent;
  const currentPhone = document.getElementById("display-phone").textContent;

  document.getElementById("modal-name").value = currentName === "배송지를 입력해주세요" ? "" : currentName;
  document.getElementById("modal-phone").value = currentPhone === "배송지를 입력해주세요" ? "" : currentPhone;
}

// 주소 모달 닫기
function closeAddressModal() {
  document.getElementById("addressModal").style.display = "none";
}

// 다음 주소 API 실행
function execDaumPostcode() {
  new daum.Postcode({
    oncomplete: function (data) {
      var roadAddr = data.roadAddress;
      var extraRoadAddr = "";

      if (data.bname !== "" && /[동|로|가]$/g.test(data.bname)) {
        extraRoadAddr += data.bname;
      }
      if (data.buildingName !== "" && data.apartment === "Y") {
        extraRoadAddr += extraRoadAddr !== "" ? ", " + data.buildingName : data.buildingName;
      }
      if (extraRoadAddr !== "") {
        extraRoadAddr = " (" + extraRoadAddr + ")";
      }

      document.getElementById("modal-postcode").value = data.zonecode;
      document.getElementById("modal-roadAddress").value = roadAddr;
      document.getElementById("modal-extraAddress").value = extraRoadAddr;
    },
  }).open();
}

// 주소 저장
function saveAddress() {
  const name = document.getElementById("modal-name").value;
  const phone = document.getElementById("modal-phone").value;
  const postcode = document.getElementById("modal-postcode").value;
  const roadAddress = document.getElementById("modal-roadAddress").value;
  const detailAddress = document.getElementById("modal-detailAddress").value;
  const extraAddress = document.getElementById("modal-extraAddress").value;

  if (!name || !phone || !postcode || !roadAddress) {
    showOrderResultModal("필수 정보를 모두 입력해주세요.", false);
    return;
  }

  // 주소 조합
  const fullAddress = roadAddress + (detailAddress ? " " + detailAddress : "") + (extraAddress ? " " + extraAddress : "");

  // 서버에 주소 정보 전송
  const addressData = {
    recipientName: name,
    recipientPhone: phone.replace(/-/g, ""), // 하이픈 제거
    address: fullAddress,
  };

  axiosInstance
    .post("/order/address", addressData)
    .then((response) => {
      // 화면에 표시
      document.getElementById("display-name").textContent = name;
      document.getElementById("display-phone").textContent = formatPhoneNumber(phone);
      document.getElementById("display-address").textContent = fullAddress;

      // 모달 닫기
      closeAddressModal();

      showSaveSuccessModal();
    })
    .catch((error) => {
      console.error("주소 저장 실패:", error);
      if (error.response && error.response.status === 401) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        showOrderResultModal("세션이 만료되었습니다. 다시 로그인해주세요.", false);
      } else {
        showOrderResultModal("배송지 저장 중 오류가 발생했습니다.", false);
      }
    });
}
