(function () {
  // ====== PortOne 초기화 ======
  if (window.IMP && typeof window.IMP.init === "function") {
    IMP.init("imp55525217"); // 가맹점 코드
  }
  const PG = "html5_inicis";

  // ====== 공통: 인증 보장 ======
  async function ensureAuth() {
    const ok = await validateUser(); // userAuth.js
    if (!ok) throw new Error("unauthorized");
  }

  // ====== PortOne 결제 트리거 (카드) ======
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

  // ===== 해시 토큰 유틸 =====
  function getPayTokenFromHash() {
    try {
      const h = new URLSearchParams(window.location.hash?.slice(1));
      return h.get("pay");
    } catch {
      return null;
    }
  }
  function setPayTokenHash(token) {
    try {
      const h = new URLSearchParams(window.location.hash?.slice(1));
      h.set("pay", token);
      window.location.hash = h.toString();
    } catch {}
  }

  // ====== 세션 컨텍스트 조회 ======
  async function fetchSessionContext(token) {
    const res = await axiosInstance.get("/payment/session/context", { params: { token } });
    if (res.data?.status !== "success") throw new Error(res.data?.message || "세션 조회 실패");
    return res.data; // {status, merchantUid, amount, title, method, token}
  }

  // 바로구매: /payment/session/start -> { status, token }
  async function createBookSession({ bookId, qty = 1, method = "CARD" }) {
    const res = await axiosInstance.post("/payment/session/start", null, { params: { bookId, qty, method } });
    const data = res.data;
    if (!data || data.status !== "success" || !data.token) throw new Error(data?.message || "세션 생성 실패(도서)");
    return data.token;
  }
  // 장바구니 합계: /payment/session/start-cart -> { status, token }
  async function createCartSession({ amount, title }) {
    const res = await axiosInstance.post("/payment/session/start-cart", null, { params: { amount, title } });
    const data = res.data;
    if (!data || data.status !== "success" || !data.token) throw new Error(data?.message || "세션 생성 실패(장바구니)");
    return data.token;
  }

  // ====== 사전 컨텍스트 주입 ======
  async function prepOrderPayContext() {
    const btn = document.getElementById("payBtn");
    if (!btn) return;

    // 이미 주입된 경우 스킵
    if (btn.dataset.merchant && btn.dataset.amount && btn.dataset.title && btn.dataset.token) return;

    // 1) 토큰 확보: 해시 재사용, 없으면 생성
    let token = getPayTokenFromHash();
    if (!token) {
      const isDirect = !!(window.bookData && window.bookData.bookId); 
      if (isDirect) {
        const bookId = String(window.bookData.bookId);
        const qty = Number(window.quantity || 1);
        token = await createBookSession({ bookId, qty, method: "CARD" });
      } else {
        // 장바구니: 총액/대표 타이틀 추출
        const totalText = (document.getElementById("total")?.textContent || "").replace(/[^\d]/g, "");
        const amount = parseInt(totalText || "0", 10) || 0;
        const count =
          Number(btn.dataset.itemCount) ||
          document.querySelectorAll("#orderItems .order-item, .order-item").length ||
          1;
        const firstTitle =
          document.querySelector("#orderItems .item-details h3, .order-item .item-title")?.textContent?.trim();
        const title = count > 1 ? `도서 ${count}건` : (firstTitle || "도서 결제");

        token = await createCartSession({ amount, title });
      }
      setPayTokenHash(token); // 새 토큰 해시에 보존
    }

    // 2) 서버 컨텍스트로 버튼 data-* 채우기
    const ctx = await fetchSessionContext(token); 
    const method = (ctx.method || "CARD").toLowerCase();

    btn.dataset.token = token;
    btn.dataset.merchant = ctx.merchantUid;
    btn.dataset.amount = String(ctx.amount);
    btn.dataset.title = ctx.title;
    btn.dataset.method = method;

    // 화면 바인딩
    document.querySelectorAll("[data-bind='title']").forEach(el => (el.textContent = ctx.title));
    document.querySelectorAll("[data-bind='amount']").forEach(el => {
      el.textContent = new Intl.NumberFormat().format(ctx.amount);
    });
  }

  // ====== 결제 완료 검증 → 성공 리다이렉트 ======
  async function completeAndRedirect({ merchantUid, impUid, token }) {
    const res = await axiosInstance.post("/payment/api/complete", { merchantUid, impUid, token });
    const data = res.data;
    if (data?.status === "success" && data?.successRedirect) {
      window.location.href = data.successRedirect;
      return;
    }
    throw new Error(data?.message || "결제 검증 실패");
  }

  // ====== 탭 UI: 기본은 카드, vbank는 클릭 시에만 표시 ======
  function setupTabs() {
    const tabs = document.querySelectorAll(".pay-method-btn");
    if (!tabs.length) return;

    const btnCard = document.querySelector('.pay-method-btn[data-method="card"]');
    const btnVbank = document.querySelector('.pay-method-btn[data-method="vbank"]');
    const vbankSection = document.getElementById("vbankSection");

    // 초기 상태: 카드 on, vbank off
    tabs.forEach(b => b.classList.remove("is-active"));
    if (btnCard) btnCard.classList.add("is-active");
    if (vbankSection) vbankSection.classList.remove("open");

    // 전환 로직
    tabs.forEach(btn => {
      btn.addEventListener("click", () => {
        tabs.forEach(b => b.classList.remove("is-active"));
        btn.classList.add("is-active");

        const method = btn.dataset.method;
        if (vbankSection) {
          if (method === "vbank") vbankSection.classList.add("open");
          else vbankSection.classList.remove("open");
        }
      });
    });
  }

  // UI 세팅
  setupTabs();

  // ====== 프래그먼트 페이지용: 버튼 바인딩 ======
  async function bindPayFragment() {
    const btn = document.getElementById("payBtn");
    if (!btn) return; // 프래그먼트가 없는 페이지

    // 이벤트예약 팝업: 결제 성공 전 버튼 비활성화
    const submitBtn = document.getElementById("submit-btn");
    const isEvent = !!submitBtn;

    if (isEvent) {
      submitBtn.disabled = true;
      submitBtn.classList.add("btn-disabled");

      // token 위치: (1) data-token on #payBtn, (2) #token input
      let token =
        btn.getAttribute("data-token") ||
        document.getElementById("token")?.value ||
        document.getElementById("pay_token")?.value;

      // 아직 버튼에 data-merchant 세팅이 없다면 → 서버에서 컨텍스트 조회해 채움
      if (!btn.getAttribute("data-merchant")) {
        if (!token) throw new Error("결제 토큰이 없습니다.");
        const ctx = await fetchSessionContext(token);
        btn.setAttribute("data-merchant", ctx.merchantUid);
        btn.setAttribute("data-amount", String(ctx.amount));
        btn.setAttribute("data-title", ctx.title);

        const itemName = document.getElementById("item_name");
        if (itemName) itemName.value = ctx.title;

        const titleEls = document.querySelectorAll("[data-bind='title']");
        titleEls.forEach((el) => (el.textContent = ctx.title));
        const amountEls = document.querySelectorAll("[data-bind='amount']");
        amountEls.forEach((el) => (el.textContent = new Intl.NumberFormat().format(ctx.amount)));
      }

      // [이벤트] 결제 버튼 클릭
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

          const done = await axiosInstance.post("/payment/api/complete_event", {
            merchantUid,
            impUid: rsp.imp_uid,
            token,
          });
          if (done.data?.status === "success" && done.data?.successRedirect) {
            const res = await axiosInstance.post("/payment/paid_after", { token });
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

      return; // ← 이벤트 분기 종료
    }

    // ---------- [주문] ----------
    try {
      await ensureAuth().catch(() => {});
      await prepOrderPayContext(); // 여기서 토큰 생성 및 data-* 주입
    } catch (preErr) {
      console.warn("사전 컨텍스트 주입 실패(주문):", preErr);
    }

    btn.addEventListener("click", async (e) => {
      try {
        e.preventDefault();
        e.stopPropagation();
        await ensureAuth();

        const miss = (id) => {
          const el = document.getElementById(id);
          return !el || !el.textContent || el.textContent.includes("배송지를 입력해주세요");
        };
        if (miss("display-name") || miss("display-phone") || miss("display-address")) {
          alert("배송지를 먼저 입력해주세요.");
          if (typeof window.openAddressModal === "function") openAddressModal();
          return;
        }

        if (!btn.dataset.merchant || !btn.dataset.amount || !btn.dataset.title || !btn.dataset.token) {
          await prepOrderPayContext();
        }

        const token = btn.dataset.token || getPayTokenFromHash();
        const merchantUid = btn.dataset.merchant;
        const amount = parseInt(btn.dataset.amount || "0", 10);
        const title = btn.dataset.title || "도서 결제";
        const method = btn.dataset.method || "card";

        if (!token || !merchantUid || !amount) {
          alert("결제 정보가 준비되지 않았습니다. 새로고침 후 다시 시도해주세요.");
          return;
        }

        const rsp = await requestPayCard({ merchantUid, amount, itemName: title, method });
        await completeAndRedirect({ merchantUid: rsp.merchant_uid, impUid: rsp.imp_uid, token });
      } catch (err) {
        console.error("[PAY][BOOK] 에러:", err);
        alert(err.message || "결제 처리 중 오류");
      }
    });
  }

  // 전역 바인딩
  window.bindPayFragment = bindPayFragment;

  // ====== (공통) 페이지에서 쉽게 부르는 시작 함수 2가지 ======
  async function startBookPayment({ bookId, qty = 1, method = "CARD" }) {
    await ensureAuth();
    const res = await axiosInstance.post("/payment/session/start", null, { params: { bookId, qty, method } });
    const data = res.data;
    if (data?.status === "success" && data?.redirectUrl) {
      window.location.href = data.redirectUrl;
      return;
    }
    throw new Error(data?.message || "결제 준비 실패");
  }

  async function startEventPayment({ eventId, amount, title, method = "CARD" }) {
    await ensureAuth();
    const res = await axiosInstance.post("/payment/session/start-event", null, { params: { eventId, amount, title, method } });
    const data = res.data;
    if (data.status === "success" && data.redirectUrl) {
      window.location.href = data.redirectUrl;
      return;
    }
    throw new Error(data.message || "결제 준비 실패");
  }

  // 전역 노출
  window.PaymentStart = { book: startBookPayment, event: startEventPayment };

  // ====== DOM 로딩 후 프래그먼트 바인딩 ======
  document.addEventListener("DOMContentLoaded", () => {
    bindPayFragment().catch(err => {
      // 프래그먼트가 없거나 토큰 이슈 등은 경고만
      if (err && /프래그먼트|토큰|세션/.test(String(err.message || ""))) {
        console.warn(err);
      }
    });
  });
})();




