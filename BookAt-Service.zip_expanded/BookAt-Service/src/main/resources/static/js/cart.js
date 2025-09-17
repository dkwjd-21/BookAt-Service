document.addEventListener("DOMContentLoaded", () => {
  // --- DOM 요소 가져오기 ---
  const form = document.querySelector('form[action="/order"]') || document.querySelector('form[action$="/order"]') || document.querySelector('form[action*="/order"]') || document.querySelector("form");
  const selectAllCheckbox = document.getElementById("select-all");

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

    if (!selectAllCheckbox) return;

    if (allItemCheckboxes.length === 0) {
      selectAllCheckbox.checked = false;
      selectAllCheckbox.disabled = true;
    } else {
      selectAllCheckbox.disabled = false;
      selectAllCheckbox.checked = allItemCheckboxes.length === checkedItemCheckboxes.length;
    }
  };

  // --- 이벤트 리스너 설정 (Event Listener Setup) ---
  // 이벤트 위임(Event Delegation)을 사용하여 form 요소 하나에만 리스너를 추가합니다.
  // 이렇게 하면 각 버튼에 개별적으로 리스너를 추가할 필요가 없어 코드가 효율적입니다.

  // [전체 선택] 체크박스 변경 시
  if (selectAllCheckbox) {
    selectAllCheckbox.addEventListener("change", () => {
      const allItemCheckboxes = document.querySelectorAll(".item-checkbox");
      allItemCheckboxes.forEach((checkbox) => {
        checkbox.checked = selectAllCheckbox.checked;
      });
      updateSummary();
    });
  }

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
    }

    // [-] 버튼
    if (target.classList.contains("quantity-minus")) {
      if (quantity > 1) {
        quantity--;
        quantityInput.value = quantity;
        updateItemPrice(cartItem);
        updateSummary();
      }
    }

    // [삭제] 버튼
    if (target.classList.contains("delete-btn")) {
      const cartId = target.dataset.cartId;
      console.log(`서버에 삭제 요청: cartId ${cartId}`);
      // TODO: fetch API 등을 사용하여 실제 서버에 삭제 요청을 보내야 합니다.
      // 예: fetch(`/api/cart/${cartId}`, { method: 'DELETE' });

      cartItem.remove(); // 화면에서 즉시 삭제
      updateSelectAllCheckboxState();
      updateSummary();
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
    }
  });

  // --- 페이지 첫 로드 시 초기 계산 실행 ---
  document.querySelectorAll(".cart-item").forEach(updateItemPrice);
  updateSelectAllCheckboxState();
  updateSummary();
});
