(function () {
  // ====== PortOne 초기화 ======
  if (window.IMP && typeof window.IMP.init === "function") {
    IMP.init("imp55525217"); // 가맹점 코드
  }
  const PG = "html5_inicis";

  // ====== 공통: 인증 보장 ======
  async function ensureAuth() {
    const ok = await validateUser(); // userAuth.js 제공
    if (!ok) throw new Error("unauthorized");
  }

  // ====== PortOne 결제 트리거 (기본 카드) ======
  function requestPayCard({ merchantUid, amount, itemName, method = "card" }) {
    return new Promise((resolve, reject) => {
      if (!window.IMP || typeof window.IMP.request_pay !== "function") {
        return reject(new Error("결제 모듈이 로드되지 않았습니다. 잠시 후 다시 시도해주세요."));
      }
      IMP.request_pay(
        {
          pg: PG,
          pay_method: method,
          name: itemName || "상품명",
          merchant_uid: merchantUid,
          amount: amount,
        },
        function (rsp) {
          if (rsp?.success) return resolve(rsp);
          return reject(new Error(rsp?.error_msg || "결제 실패"));
        }
      );
    });
  }

  // ===== 해시 토큰 =====
  function setPayTokenHash(token) {
    try {
      const h = new URLSearchParams(window.location.hash?.slice(1));
      h.set("pay", token);
      window.location.hash = h.toString();
    } catch {}
  }

  // ===== 주문/세션 유틸 =====
  function getOrderIdFromPage() {
    const id1 = document.getElementById("orderId")?.value;
    if (id1 && /^\d+$/.test(id1)) return parseInt(id1, 10);

    const id2 = document.body?.dataset?.orderId;
    if (id2 && /^\d+$/.test(id2)) return parseInt(id2, 10);

    return null;
  }

  const PAY_SESSION_KEY = (orderId) => `pay_session_${orderId}`;
  function getStoredPayToken(orderId) {
    try { return sessionStorage.getItem(PAY_SESSION_KEY(orderId)) || null; } catch { return null; }
  }
  function storePayToken(orderId, token) {
    try { sessionStorage.setItem(PAY_SESSION_KEY(orderId), token); } catch {}
  }
  function clearPayToken(orderId) {
    try { sessionStorage.removeItem(PAY_SESSION_KEY(orderId)); } catch {}
  }

  // ====== 세션 컨텍스트 조회 ======
  async function fetchSessionContext(token) {
    const res = await window.axiosInstance.get("/payment/session/context", { params: { token } });
    if (res.data?.status !== "success") throw new Error(res.data?.message || "세션 조회 실패");
    return res.data;
  }

  // ===== 주문 금액 계산 =====
  function collectCartPayload() {
    let stateItems = [];
    try {
      if (typeof orderItems !== "undefined" && Array.isArray(orderItems) && orderItems.length) {
        stateItems = orderItems;
      } else if (Array.isArray(window.orderItems) && window.orderItems.length) {
        stateItems = window.orderItems;
      }
    } catch (e) {}

    if (stateItems.length) {
      const subtotal = stateItems.reduce(
        (sum, it) => sum + Number(it.price) * Number(it.quantity ?? 1),
        0
      );
      const shippingFee = subtotal > 0 && subtotal < 15000 ? 3000 : 0;
      const totalAmount = subtotal + shippingFee;
      return { subtotal, shippingFee, totalAmount, items: stateItems };
    }

    // DOM
    const items = Array.from(document.querySelectorAll("#orderItems .order-item, .order-item"));
    let subtotal = 0;
    items.forEach((el) => {
      const price = Number(
        (
          el.querySelector("[data-price]")?.getAttribute("data-price") ||
          el.querySelector(".price")?.textContent ||
          "0"
        )
          .toString()
          .replace(/[^\d]/g, "")
      );
      const qty =
        Number(
          (
            el.querySelector("[data-qty]")?.getAttribute("data-qty") ||
            el.querySelector(".quantity")?.textContent ||
            "1"
          )
            .toString()
            .replace(/[^\d]/g, "")
        ) || 1;
      subtotal += price * qty;
    });
    const shippingFee = subtotal > 0 && subtotal < 15000 ? 3000 : 0;
    const totalAmount = subtotal + shippingFee;
    return { subtotal, shippingFee, totalAmount, items: [] };
  }

  // 서버가 기대하는 바디로 변환: { items: [...] }
  function buildOrderCreateBody() {
    const { items } = collectCartPayload();
    if (items && items.length) {
      return {
        items: items.map(it => ({
          // cart에서 오면 cartId 존재, 바로구매면 없어도 됨
          cartId: it.cartId ?? it.cartID ?? it.id ?? null,
          bookId: Number(it.bookId),
          price: Number(it.price),
          quantity: Number(it.quantity ?? it.cartQuantity ?? 1),
        }))
      };
    }
	  // DOM 
	  const nodes = Array.from(document.querySelectorAll("#orderItems .order-item"));
	  const domItems = nodes.map(el => {
	    const priceText = el.querySelector(".price")?.textContent || "0";
	    const qtyText   = el.querySelector(".quantity")?.textContent || "1";
	    const price = Number(priceText.toString().replace(/[^\d]/g, ""));
	    const quantity = Number(qtyText.toString().replace(/[^\d]/g, "")) || 1;
	    return { cartId: null, bookId: 0, price, quantity };
	  });
	  return { items: domItems };
	}

	// ===== 주문 선생성 API (클릭 시 한 번만)
	async function createOrderOnServerOnce() {
	  const exists = getOrderIdFromPage();
	  if (exists) return exists;

	  const body = buildOrderCreateBody();
	  if (!body.items || body.items.length === 0) {
	    throw new Error("주문할 상품을 찾지 못했습니다. 장바구니에서 선택 후 들어와주세요.");
	  }

	  // POST /order/create
	  const res = await window.axiosInstance.post("/order/create", body, {
	    headers: { "Content-Type": "application/json" }
	  });
	  const data = res.data || {};
	  const orderId = data.orderId;
	  if (!orderId) {
	    throw new Error(data?.message || "주문번호를 찾을 수 없습니다.");
	  }

	  // hidden/dataset에 심어두기
	  let hid = document.getElementById("orderId");
	  if (!hid) {
	    hid = document.createElement("input");
	    hid.type = "hidden";
	    hid.id = "orderId";
	    document.body.appendChild(hid);
	  }
	  hid.value = String(orderId);
	  try { document.body.dataset.orderId = String(orderId); } catch {}

	  return orderId;
	}

  // 세션 생성 
  async function createCartSessionHash({ amount, title, orderId }) {
    const params = { amount, title, orderId };
    const res = await window.axiosInstance.post("/payment/session/start-cart", null, { params });
    const data = res.data || {};
    if (data.status !== "success" || !data.redirectUrl) throw new Error("세션 생성 실패(장바구니)");
    const hash = data.redirectUrl.split("#")[1] || "";
    const token = new URLSearchParams(hash).get("pay");
    if (!token) throw new Error("세션 토큰이 없습니다.");
    return token;
  }

  // 탭 UI
  function initPayTabs(context = document) {
    const roots = (context.querySelectorAll ? context.querySelectorAll(".pay-tabs")
                                            : document.querySelectorAll(".pay-tabs"));
    if (!roots.length) return;

    roots.forEach((root) => {
      if (!root || root.dataset.init === "1") return;  // 이미 초기화된 경우 스킵
      const btnCard = root.querySelector('.pay-method-btn[data-method="card"]');
      const vbank   = root.querySelector("#vbankSection");
      root.querySelectorAll(".pay-method-btn").forEach(b => b.classList.remove("is-active"));
      if (btnCard) btnCard.classList.add("is-active");
      if (vbank)   vbank.classList.remove("open");
      root.dataset.init = "1";
    });
  }

  // 탭 클릭
  document.addEventListener("click", (e) => {
    const btn = e.target.closest(".pay-method-btn");
    if (!btn) return;
    const root = btn.closest(".pay-tabs");
    if (!root) return;
    root.querySelectorAll(".pay-method-btn").forEach(b => b.classList.remove("is-active"));
    btn.classList.add("is-active");
    const vbank = root.querySelector("#vbankSection");
    if (vbank) {
      if (btn.dataset.method === "vbank") vbank.classList.add("open");
      else vbank.classList.remove("open");
    }
  });

  // ====== 프래그먼트 페이지용: 버튼 바인딩 ======
  async function bindPayFragment() {
    initPayTabs();
    const btn = document.getElementById("payBtn");
    if (!btn) return; // 프래그먼트가 없는 페이지

    // 이벤트예약 팝업 존재 여부로 분기
    const submitBtn = document.getElementById("submit-btn");
    const isEvent = !!submitBtn;

    if (isEvent) {
      submitBtn.disabled = true;
      submitBtn.classList.add("btn-disabled");

      let token =
        btn.getAttribute("data-token") ||
        document.getElementById("token")?.value ||
        document.getElementById("pay_token")?.value;

      if (!btn.getAttribute("data-merchant")) {
        if (!token) throw new Error("결제 토큰이 없습니다.");
        const ctx = await fetchSessionContext(token);
        btn.setAttribute("data-merchant", ctx.merchantUid);
        btn.setAttribute("data-amount", String(ctx.amount));
        btn.setAttribute("data-title", ctx.title);

        const itemName = document.getElementById("item_name");
        if (itemName) itemName.value = ctx.title;

        document.querySelectorAll("[data-bind='title']").forEach((el) => (el.textContent = ctx.title));
        document.querySelectorAll("[data-bind='amount']").forEach(
          (el) => (el.textContent = new Intl.NumberFormat().format(ctx.amount))
        );
      }

      btn.addEventListener("click", async (e) => {
        try {
          e.preventDefault();
          e.stopPropagation();
          await ensureAuth();

          const merchantUid = btn.getAttribute("data-merchant");
          const amount = parseInt(btn.getAttribute("data-amount") || "0", 10);
          const title = btn.getAttribute("data-title") || "상품명";
          let token =
            btn.getAttribute("data-token") ||
            document.getElementById("token")?.value ||
            document.getElementById("pay_token")?.value;

          if (!merchantUid || !amount) {
            alert("결제 정보가 준비되지 않았습니다. 새로고침 후 다시 시도해주세요.");
            return;
          }

          const rsp = await requestPayCard({ merchantUid, amount, itemName: title });

          const done = await window.axiosInstance.post("/payment/api/complete_event", {
            merchantUid,
            impUid: rsp.imp_uid,
            token,
          });
          if (done.data?.status === "success" && done.data?.successRedirect) {
            const res = await window.axiosInstance.post("/payment/paid_after", { token });
            if (res.data?.status === "success") {
              submitBtn.disabled = false;
              submitBtn.classList.remove("btn-disabled");
            } else {
              console.log("예약 저장 실패 : ", res.data?.message);
            }
          } else {
            throw new Error(done.data?.message || "결제 검증 실패");
          }
        } catch (e2) {
          console.error("[PAY][EVENT] 에러:", e2);
          alert(e2.message || "결제 처리 중 오류");
        }
      });

      return; // 이벤트 분기 종료
    }

    // ---------- 주문 페이지 ----------
    btn.addEventListener("click", async (e) => {
      // 중복 클릭 방지
      if (btn.dataset.busy === "1") return;
      btn.dataset.busy = "1";
      btn.classList.add("btn-disabled");

      try {
        e.preventDefault();
        e.stopPropagation();
        await ensureAuth();

        // 배송지 체크
        const miss = (id) => {
          const el = document.getElementById(id);
          return !el || !el.textContent || el.textContent.includes("배송지를 입력해주세요");
        };
        if (miss("display-name") || miss("display-phone") || miss("display-address")) {
          alert("배송지를 먼저 입력해주세요.");
          if (typeof window.openAddressModal === "function") openAddressModal();
          return;
        }

        // 대표 타이틀/금액
        const count =
          Number(btn.dataset.itemCount) ||
          document.querySelectorAll("#orderItems .order-item, .order-item").length ||
          1;
        const firstTitle =
          document
            .querySelector("#orderItems .item-details h3, .order-item .item-title")
            ?.textContent?.trim();
        const title = count > 1 ? `도서 ${count}건` : firstTitle || "도서 결제";

		// 주문 생성
		let orderId = getOrderIdFromPage();
		if (!orderId) {
		  orderId = await createOrderOnServerOnce();
		}

        const { totalAmount } = collectCartPayload();

        // 결제 세션 토큰
        let token = getStoredPayToken(orderId);
        if (!token) {
          token = await createCartSessionHash({ amount: totalAmount, title, orderId });
          storePayToken(orderId, token);
        }
        setPayTokenHash(token);

        const ctx = await fetchSessionContext(token);
        btn.dataset.token = token;
        btn.dataset.merchant = ctx.merchantUid;
        btn.dataset.amount = String(ctx.amount);
        btn.dataset.title = ctx.title;
        btn.dataset.method = (ctx.method || "CARD").toLowerCase();

        const rsp = await requestPayCard({
          merchantUid: ctx.merchantUid,
          amount: ctx.amount,
          itemName: ctx.title,
          method: btn.dataset.method,
        });

		//완료 검증
        const done = await window.axiosInstance.post("/payment/api/complete", {
          merchantUid: rsp.merchant_uid,
          impUid: rsp.imp_uid,
          token,
        });
        if (done.data?.status === "success" && done.data?.successRedirect) {
          clearPayToken(orderId); // 성공 시 세션 토큰 제거
          window.location.href = done.data.successRedirect;
        } else {
          throw new Error(done.data?.message || "결제 검증 실패");
        }
      } catch (err) {
        console.error("[PAY] 에러:", err);
        alert(err.response?.data?.message || err.message || "결제 처리 중 오류");
      } finally {
        // 실패 시에만 다시 클릭 가능하도록
        if (!/successRedirect/.test(String(window.location.href))) {
          btn.dataset.busy = "0";
          btn.classList.remove("btn-disabled");
        }
      }
    });
  }

  // 전역 바인딩
  window.bindPayFragment = bindPayFragment;

  // ====== 페이지에서 쉽게 부르는 시작 함수 ======
  async function startEventPayment({ eventId, amount, title, method = "CARD" }) {
    await ensureAuth();
    const res = await window.axiosInstance.post("/payment/session/start-event", null, {
      params: { eventId, amount, title, method },
    });
    const data = res.data;
    if (data?.status === "success" && data?.redirectUrl) {
      window.location.href = data.redirectUrl;
      return;
    }
    throw new Error(data?.message || "결제 준비 실패");
  }

  // 전역 노출
  window.PaymentStart = window.PaymentStart || {};
  window.PaymentStart.event = startEventPayment;

  // ====== DOM 로딩 후 프래그먼트 바인딩 ======
  document.addEventListener("DOMContentLoaded", () => {
    initPayTabs();
    if (!window.axiosInstance) console.warn("axiosInstance not initialized");

    bindPayFragment().catch((err) => {
      if (err && /프래그먼트|토큰|세션/.test(String(err.message || ""))) {
        console.warn(err);
      }
    });
  });
})();


