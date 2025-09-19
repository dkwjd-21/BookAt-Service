// 주문 상품 정보를 저장할 변수
let orderItems = [];

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
document.addEventListener("DOMContentLoaded", function () {
  // 로그인 검증
  checkLoginStatus();

  // 전화번호 포맷팅 적용
  formatPhoneNumbers();

  // 세션스토리지에서 주문 상품 정보를 가져옴 (URL 파라미터 대신)
  const itemsData = sessionStorage.getItem("orderItems");

  if (itemsData) {
    try {
      orderItems = JSON.parse(itemsData);
      renderOrderItems();
      updateOrderSummary();
      // 사용 후 세션스토리지에서 제거
      sessionStorage.removeItem("orderItems");
    } catch (e) {
      console.error("주문 상품 정보를 파싱하는 중 오류가 발생했습니다:", e);
      alert("주문 상품 정보를 불러올 수 없습니다.");
      window.location.href = "/cart";
    }
  } else {
    alert("주문할 상품이 없습니다.");
    window.location.href = "/cart";
  }

  // 결제하기 버튼 클릭 이벤트
  document.getElementById("paymentBtn").addEventListener("click", function () {
    const accessToken = localStorage.getItem("accessToken");

    if (!accessToken) {
      alert("로그인이 필요합니다.");
      window.location.href = "/user/login";
      return;
    }

    // 주문 생성 요청
    const cartIds = orderItems.map((item) => item.cartId || item.bookId); // cartId가 없으면 bookId 사용

    // 배송비 포함 총 금액 계산
    const subtotal = orderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
    const shippingFee = subtotal > 0 && subtotal < 15000 ? 3000 : 0;
    const totalAmount = subtotal + shippingFee;

    console.log("=== 주문 요청 시작 ===");
    console.log("cartIds:", cartIds);
    console.log("subtotal:", subtotal);
    console.log("shippingFee:", shippingFee);
    console.log("totalAmount:", totalAmount);

    axiosInstance
      .post("/order/create", {
        cartIds: cartIds,
        subtotal: subtotal,
        shippingFee: shippingFee,
        totalAmount: totalAmount,
      })
      .then((response) => {
        console.log("=== 주문 응답 받음 ===");
        console.log("response:", response);
        console.log("response.data:", response.data);

        if (response.data) {
          alert(response.data);
          if (response.data === "주문이 완료되었습니다.") {
            // 주문 완료 후 장바구니나 메인페이지로 이동
            window.location.href = "/cart";
          }
        }
      })
      .catch((error) => {
        console.error("=== 주문 처리 중 오류 ===");
        console.error("error:", error);
        console.error("error.response:", error.response);

        if (error.response && error.response.status === 401) {
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          alert("세션이 만료되었습니다. 다시 로그인해주세요.");
          window.location.href = "/user/login";
        } else {
          alert("주문 처리 중 오류가 발생했습니다.");
        }
      });
  });
});

// 로그인 상태 확인 함수 (서버에서 이미 검증했으므로 간단히 체크)
function checkLoginStatus() {
  const accessToken = localStorage.getItem("accessToken");

  if (!accessToken) {
    alert("로그인이 필요합니다.");
    window.location.href = "/user/login";
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

// 주소 모달 관련 함수들

// 주소 모달 열기
function openAddressModal() {
  document.getElementById("addressModal").style.display = "block";
  // 현재 값으로 모달 필드 초기화
  document.getElementById("modal-name").value = document.getElementById("display-name").textContent;
  document.getElementById("modal-phone").value = document.getElementById("display-phone").textContent;
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
    alert("필수 정보를 모두 입력해주세요.");
    return;
  }

  // 주소 조합
  const fullAddress = roadAddress + (detailAddress ? " " + detailAddress : "") + (extraAddress ? " " + extraAddress : "");

  // 화면에 표시
  document.getElementById("display-name").textContent = name;
  document.getElementById("display-phone").textContent = formatPhoneNumber(phone);
  document.getElementById("display-address").textContent = fullAddress;

  // 모달 닫기
  closeAddressModal();
}
