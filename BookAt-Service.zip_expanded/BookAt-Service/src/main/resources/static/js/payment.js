(function () {
  // ====== PortOne 초기화 ======
  if (window.IMP && typeof window.IMP.init === "function") {

    IMP.init("imp55525217");
  }
  const PG = "html5_inicis";

  // ====== 공통: 인증 ======
  async function ensureAuth() {
    const ok = await validateUser(); // userAuth.js
    if (!ok) throw new Error("unauthorized");
  }

  // ====== 실패/취소 서버 보고 ======
  async function reportFail(merchantUid, reason) {
    try {
      if (!merchantUid) return;
      await axiosInstance.post("/payment/fail", {
        merchantUid,
        reason: reason || "결제실패/사용자취소",
      });
    } catch (e) {
      console.warn("[PAY] /payment/fail 보고 실패", e);
    }
  }

  // ====== 세션 컨텍스트 가져오기 ======
  async function fetchSessionContext(token) {
    const res = await axiosInstance.get("/payment/session/context", { params: { token } });
    if (res.data?.status !== "success") {
      throw new Error(res.data?.message || "세션 조회 실패");
    }
    return res.data; // {merchantUid, amount, title, method, token}
  }

  // ====== PortOne 결제 트리거 (카드 고정) ======
  function requestPayCard({ merchantUid, amount, itemName }) {
    return new Promise((resolve, reject) => {
      IMP.request_pay(
        {
          pg: PG,
          pay_method: "card", 
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

  // ====== UI: 기본은 카드, vbank는 클릭 시에만 표시 ======
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

  // ====== 프래그먼트 페이지용 ======
  async function bindPayFragment() {
    const btn = document.getElementById("payBtn");
    if (!btn) return; // 프래그먼트가 없는 페이지

    // UI 세팅
    setupTabs();

    // token 위치: (1) data-token on #payBtn, (2) #token input
    let token = btn.getAttribute("data-token")
      || document.getElementById("token")?.value
      || document.getElementById("pay_token")?.value;

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
      titleEls.forEach(el => el.textContent = ctx.title);
      const amountEls = document.querySelectorAll("[data-bind='amount']");
      amountEls.forEach(el => el.textContent = new Intl.NumberFormat().format(ctx.amount));
    }

    // 결제 버튼 클릭
    btn.addEventListener("click", async (e) => {
      let merchantUid, amount, title;
      try {
        e.preventDefault();
        e.stopPropagation();

        console.log("[PAY] click");

        await ensureAuth(); // /auth/validate OK

        merchantUid = btn.getAttribute("data-merchant");
        amount = parseInt(btn.getAttribute("data-amount") || "0", 10);
        title = btn.getAttribute("data-title") || "상품명";
        token = btn.getAttribute("data-token")
          || document.getElementById("token")?.value
          || document.getElementById("pay_token")?.value;

        if (!merchantUid || !amount) {
          console.error("[PAY] 결제정보 부족", { merchantUid, amount });
          alert("결제 정보가 준비되지 않았습니다. 새로고침 후 다시 시도해주세요.");
          return;
        }

        if (!window.IMP || typeof window.IMP.request_pay !== "function") {
          console.error("[PAY] PortOne SDK 미로딩");
          alert("결제 모듈이 로드되지 않았습니다. 잠시 후 다시 시도해주세요.");
          return;
        }

        console.log("[PAY] request_pay 호출", { merchantUid, amount, title });
        // PortOne 결제창
        await new Promise((resolve, reject) => {
          IMP.request_pay(
            {
              pg: "html5_inicis",
              pay_method: "card",
              name: title,
              merchant_uid: merchantUid,
              amount: amount,
            },
            async function (r) {
              console.log("[PAY] request_pay 응답", r);
              if (r?.success) {
                return resolve(r);
              }
              // 실패/사용자 취소: 여기서 -1로 보고
              await reportFail(merchantUid, r?.error_msg || "결제가 취소되었습니다. 다시 진행해 주세요.");
			  err.code = r?.error_code || "USER_CANCELED";
			  return reject(err);
            }
          );
        }).then(async (rsp) => {
          console.log("[PAY] complete 호출", { imp_uid: rsp.imp_uid });
          const done = await axiosInstance.post("/payment/api/complete", { merchantUid, impUid: rsp.imp_uid, token });
          if (done.data?.status === "success" && done.data?.successRedirect) {
            window.location.href = done.data.successRedirect;
          } else {
            throw new Error(done.data?.message || "결제 검증 실패");
          }
        });
      } catch (e2) {
        console.error("[PAY] 에러:", e2);
        // 결제창 뜨기 전/후 모든 예외 케이스에서 한번 더 실패 보고
        await reportFail(merchantUid, e2?.message || "클라이언트 오류로 결제 실패");
        alert(e2.message || "결제 처리 중 오류");
      }
    });
  }

  // ====== (공통) 페이지에서 쉽게 부르는 시작 함수 2가지 ======
  // 추후 화면에서 계산된 합계로 가져와야함
  async function startBookPayment({ bookId, qty = 1, method = "CARD" }) {
    await ensureAuth();
    const res = await axiosInstance.post("/payment/session/start", null, { params: { bookId, qty, method } });
    const data = res.data;
    if (data.status === "success" && data.redirectUrl) {
      window.location.href = data.redirectUrl;
      return;
    }
    throw new Error(data.message || "결제 준비 실패");
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

  // ====== 전역 노출 ======
  window.PaymentStart = {
    book: startBookPayment,
    event: startEventPayment
  };

  // ====== DOM 로딩 후 프래그먼트 바인딩 ======
  document.addEventListener("DOMContentLoaded", () => {
    bindPayFragment().catch(err => {
      // 프래그먼트가 없거나 토큰이 없는 경우
      if (err && /프래그먼트|토큰/.test(String(err.message || ""))) {
        console.warn(err);
      }
    });
  });
})();


