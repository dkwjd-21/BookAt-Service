document.addEventListener("DOMContentLoaded", async () => {
  let debounceTimer;

  const updateQuantityOnServer = (cartId, quantity) => {
    axios
      .put(`/cart/api/${cartId}`, { quantity })
      .then((response) => {
        if (response.status !== 200) {
          console.error("수량 변경 서버 응답 실패:", response);
          alert("수량 변경에 실패했습니다.");
        }
      })
      .catch((error) => {
        console.error("Error updating quantity:", error);
        alert("수량 변경 중 오류가 발생했습니다.");
      });
  };

  const debouncedUpdate = (cartItem) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const cartId = cartItem.dataset.cartId;
      const quantity = parseInt(cartItem.querySelector(".quantity-input").value);
      updateQuantityOnServer(cartId, quantity);
    }, 500); // 500ms debounce delay
  };

  // --- DOM 요소 가져오기 ---
  const form = document.querySelector('form[action="/order"]') || document.querySelector('form[action$="/order"]') || document.querySelector('form[action*="/order"]') || document.querySelector("form");
  const selectAllCheckbox = document.getElementById("select-all");
  const cartItemsContainer = document.querySelector(".cart-items-container");

  // --- 비동기 데이터 로딩 함수 ---
  const fetchCartItems = async () => {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
      cartItemsContainer.innerHTML = `
        <section class="cart-section">
          <h2>장바구니</h2>
          <div class="empty-cart-message">
            <p>장바구니는 로그인 후 이용 가능합니다.</p>
            <a href="/user/login" class="login-link">로그인 하러 가기</a>
          </div>
        </section>`;
      updateSummary();
      updateSelectAllCheckboxState();
      return;
    }

    try {
      const response = await fetch("/cart/api", {
        // API 경로 수정
        method: "GET",
        headers: {
          Authorization: "Bearer " + accessToken,
        },
      });

      if (response.status === 401 || response.status === 403) {
        // 토큰이 유효하지 않거나 만료된 경우
        localStorage.removeItem("accessToken");
        alert("로그인 정보가 만료되었습니다. 다시 로그인해주세요.");
        window.location.href = "/user/login";
        return;
      }

      if (!response.ok) {
        throw new Error("장바구니 정보를 불러오는데 실패했습니다.");
      }

      const cartItems = await response.json();

      if (!cartItems || cartItems.length === 0) {
        // 장바구니가 비어있는 경우
        cartItemsContainer.innerHTML = `
        <section class="cart-section">
          <h2>장바구니</h2>
          <div class="empty-cart-message">
            <p>장바구니가 비어 있습니다.</p>
          </div>
        </section>`;
      } else {
        // 장바구니에 상품이 있는 경우
        const itemsHtml = cartItems
          .map(
            (item) => `
            <div class="cart-item" data-price="${item.price}" data-cart-id="${item.cartId}" data-book-id="${item.bookId}">
              <div class="item-selector">
                <input type="checkbox" class="item-checkbox" name="selectedCartIds" value="${item.cartId}" checked />
              </div>
              <div class="cart-item-image">
                <img src="${item.coverImage}" alt="Product Image" />
              </div>
              <div class="cart-item-details">
                <div class="item-info">
                  <h3>${item.title}</h3>
                  <p>${item.author}</p>
                </div>
                <div class="item-actions">
                  <div class="quantity-control">
                    <button type="button" class="quantity-minus">-</button>
                    <input type="number" class="quantity-input" name="quantity" value="${item.cartQuantity}" min="1" />
                    <button type="button" class="quantity-plus">+</button>
                  </div>
                  <div class="item-price-delete">
                    <span class="item-price">${(item.price * item.cartQuantity).toLocaleString()} 원</span>
                    <button type="button" class="delete-btn" data-cart-id="${item.cartId}">🗑️</button>
                  </div>
                </div>
              </div>
            </div>`
          )
          .join("");

        cartItemsContainer.innerHTML = `
          <section class="cart-section">
            <h2>장바구니</h2>
            <div class="select-all-container">
              <input type="checkbox" id="select-all" checked />
              <label for="select-all">전체선택</label>
            </div>
            <div class="cart-items">
              ${itemsHtml}
            </div>
          </section>`;
      }

      // --- 페이지 첫 로드 시 초기 계산 실행 ---
      document.querySelectorAll(".cart-item").forEach(updateItemPrice);
      updateSelectAllCheckboxState();
      updateSummary();
    } catch (error) {
      console.error("Error fetching cart items:", error);
      cartItemsContainer.innerHTML = `
        <section class="cart-section">
          <h2>장바구니</h2>
          <div class="empty-cart-message"><p>오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p></div>
        </section>`;
    }
  };

  // --- 함수 정의 (Function Definitions) ---

  /**
   * 개별 상품 라인의 가격을 업데이트합니다. (수량 * 단가)
   * @param {HTMLElement} cartItem - 가격을 업데이트할 .cart-item 요소
   */
  const updateItemPrice = (cartItem) => {
    const price = parseFloat(cartItem.dataset.price);
    const quantity = parseInt(cartItem.querySelector(".quantity-input").value);
    const itemPriceElement = cartItem.querySelector(".item-price");

    const totalItemPrice = price * quantity;
    itemPriceElement.textContent = `${totalItemPrice.toLocaleString()} 원`;
  };

  /**
   * '주문 내역' 섹션의 전체 금액(상품 금액, 배송비, 총 결제 금액)을 업데이트합니다.
   * 체크된 상품들만 계산에 포함됩니다.
   */
  const updateSummary = () => {
    const checkedItems = document.querySelectorAll(".item-checkbox:checked");
    let subtotal = 0;

    checkedItems.forEach((checkbox) => {
      const cartItem = checkbox.closest(".cart-item");
      const price = parseFloat(cartItem.dataset.price);
      const quantity = parseInt(cartItem.querySelector(".quantity-input").value);
      subtotal += price * quantity;
    });

    // 배송비 정책: 15,000원 미만은 3,000원, 그 이상은 무료
    const shippingFee = subtotal > 0 && subtotal < 15000 ? 3000 : 0;
    const totalAmount = subtotal + shippingFee;

    document.getElementById("subtotal").textContent = `${subtotal.toLocaleString()} 원`;
    document.getElementById("shipping-fee").textContent = shippingFee === 0 ? "무료" : `${shippingFee.toLocaleString()} 원`;
    document.getElementById("total-amount").textContent = `${totalAmount.toLocaleString()} 원`;
  };

  /**
   * '전체선택' 체크박스의 상태를 현재 상품 선택 상태에 맞게 업데이트합니다.
   */
  const updateSelectAllCheckboxState = () => {
    const allItemCheckboxes = document.querySelectorAll(".item-checkbox");
    const checkedItemCheckboxes = document.querySelectorAll(".item-checkbox:checked");
    const selectAllCheckbox = document.getElementById("select-all"); // 함수 내에서 다시 찾기

    if (!selectAllCheckbox) return;

    if (allItemCheckboxes.length === 0) {
      selectAllCheckbox.checked = false;
      selectAllCheckbox.disabled = true;
    } else {
      selectAllCheckbox.disabled = false;
      selectAllCheckbox.checked = allItemCheckboxes.length === checkedItemCheckboxes.length;
    }
  };

  // --- 이벤트 리스너 설정 (일부 수정) ---

  // '전체선택'은 동적으로 생성되므로, 이벤트 위임 방식으로 변경
  document.addEventListener("change", (event) => {
    if (event.target.id === "select-all") {
      const selectAllCheckbox = event.target;
      const allItemCheckboxes = document.querySelectorAll(".item-checkbox");
      allItemCheckboxes.forEach((checkbox) => {
        checkbox.checked = selectAllCheckbox.checked;
      });
      updateSummary();
    }
  });

  // [개별 상품] 체크박스 변경 시
  document.addEventListener("change", (event) => {
    if (event.target.classList.contains("item-checkbox")) {
      updateSelectAllCheckboxState();
      updateSummary();
    }
  });

  // [수량 변경] 또는 [삭제] 버튼 클릭 시
  document.addEventListener("click", (event) => {
    const target = event.target;
    const cartItem = target.closest(".cart-item");
    if (!cartItem) return;

    const quantityInput = cartItem.querySelector(".quantity-input");
    let quantity = parseInt(quantityInput.value);

    // [+] 버튼
    if (target.classList.contains("quantity-plus")) {
      quantity++;
      quantityInput.value = quantity;
      updateItemPrice(cartItem);
      updateSummary();
      debouncedUpdate(cartItem);
    }

    // [-] 버튼
    if (target.classList.contains("quantity-minus")) {
      if (quantity > 1) {
        quantity--;
        quantityInput.value = quantity;
        updateItemPrice(cartItem);
        updateSummary();
        debouncedUpdate(cartItem);
      }
    }

    // [삭제] 버튼
    if (target.classList.contains("delete-btn")) {
      const cartId = target.dataset.cartId;
      if (confirm("이 상품을 장바구니에서 삭제하시겠습니까?")) {
        axios
          .delete(`/cart/api/${cartId}`)
          .then((response) => {
            if (response.status === 200) {
              cartItem.remove();
              updateSelectAllCheckboxState();
              updateSummary();
            } else {
              alert("삭제에 실패했습니다. 다시 시도해주세요.");
            }
          })
          .catch((error) => {
            console.error("Error deleting item:", error);
            alert("삭제 중 오류가 발생했습니다.");
          });
      }
    }
  });

  // [수량] 직접 입력 시
  document.addEventListener("input", (event) => {
    const target = event.target;
    if (target.classList.contains("quantity-input")) {
      // 숫자가 아니거나 1보다 작으면 1로 강제
      if (parseInt(target.value) < 1 || target.value === "" || isNaN(target.value)) {
        target.value = 1;
      }
      const cartItem = target.closest(".cart-item");
      updateItemPrice(cartItem);
      updateSummary();
      debouncedUpdate(cartItem);
    }
  });

  // --- 주문하기 버튼 클릭 이벤트 ---
  document.addEventListener("click", (event) => {
    if (event.target.id === "orderBtn") {
      const checkedItems = document.querySelectorAll(".item-checkbox:checked");
      if (checkedItems.length === 0) {
        alert("주문할 상품을 선택해주세요.");
        return;
      }

      const selectedCartIds = Array.from(checkedItems).map((checkbox) => checkbox.value);
      const orderItems = [];

      checkedItems.forEach((checkbox) => {
        const cartItem = checkbox.closest(".cart-item");
        const price = parseFloat(cartItem.dataset.price);
        const quantity = parseInt(cartItem.querySelector(".quantity-input").value);
        const title = cartItem.querySelector("h3").textContent;
        const author = cartItem.querySelector("p").textContent;
        const coverImage = cartItem.querySelector("img").src;

        orderItems.push({
          cartId: checkbox.value,
          bookId: cartItem.dataset.bookId,
          title: title,
          author: author,
          price: price,
          quantity: quantity,
          coverImage: coverImage,
        });
      });

      // 주문할 상품 정보를 세션스토리지에 저장 (URL에 노출되지 않음)
      sessionStorage.setItem("orderItems", JSON.stringify(orderItems));

      // 디버깅을 위한 로그
      console.log("=== 장바구니에서 주문페이지로 이동 ===");
      console.log("현재 로컬스토리지 액세스토큰:", localStorage.getItem("accessToken"));
      console.log("주문할 상품들:", orderItems);

      // 토큰을 포함한 요청으로 주문페이지 접근
      const accessToken = localStorage.getItem("accessToken");
      if (!accessToken) {
        alert("로그인이 필요합니다.");
        window.location.href = "/user/login";
        return;
      }

      // Authorization 헤더를 포함한 요청으로 주문페이지 접근
      fetch("/order", {
        method: "GET",
        headers: {
          Authorization: "Bearer " + accessToken,
          "Content-Type": "application/json",
        },
      })
        .then((response) => {
          if (response.redirected) {
            // 리다이렉트된 경우 (로그인 페이지로)
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            alert("세션이 만료되었습니다. 다시 로그인해주세요.");
            window.location.href = "/user/login";
          } else if (response.ok) {
            // 성공적으로 응답을 받으면 페이지 내용을 현재 페이지에 표시
            return response.text();
          } else {
            throw new Error("주문페이지 접근 실패");
          }
        })
        .then((html) => {
          if (html) {
            // 현재 페이지를 주문페이지 내용으로 교체
            document.open();
            document.write(html);
            document.close();
          }
        })
        .catch((error) => {
          console.error("주문페이지 접근 중 오류:", error);
          alert("주문페이지 접근 중 오류가 발생했습니다.");
        });
    }
  });

  // --- 페이지 로드 시 장바구니 데이터 가져오기 실행 ---
  fetchCartItems();
});
