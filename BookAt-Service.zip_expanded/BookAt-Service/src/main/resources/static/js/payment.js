/* =========================================================
 * ORDER  : start-order  → session/context → IMP → /payment/api/complete
 * EVENT  : start-event  → session/context → IMP → /payment/api/complete
 * Button : #payBtn
 * 총합계 id는 #totalAmount 기준
 * ========================================================= */

(function () {
  /*** 초기화 타이밍 이슈 해결) ***/
  async function waitFor(condFn, timeoutMs = 7000, intervalMs = 120) {
    const start = Date.now();
    return new Promise((resolve, reject) => {
      const t = setInterval(() => {
        try {
          if (condFn()) { clearInterval(t); return resolve(true); }
          if (Date.now() - start > timeoutMs) { clearInterval(t); return reject(new Error("WAIT_TIMEOUT")); }
        } catch (e) { clearInterval(t); reject(e); }
      }, intervalMs);
    });
  }

  let _impInited = false;
  function ensureImpInit() {
    const IMP = window.IMP;
    if (!IMP || typeof IMP.init !== "function") return;
    if (_impInited) return;
    IMP.init("imp55525217");
    _impInited = true;
  }

  /*** 엔트리 ***/
  document.addEventListener("DOMContentLoaded", async () => {
    const isEvent = !!document.getElementById("reservationToken");

    try {
      // 프래그먼트 삽입/합계 계산이 늦게 끝나는 경우를 대비해, 총합계가 0 초과가 될 때까지 기다렸다가 초기화
      await waitFor(() => {
        const btn = document.getElementById("payBtn");
        const el = document.getElementById("totalAmount");
        const amt = el ? parseInt(String(("value" in el ? el.value : el.textContent) || "0").replace(/[^\d]/g, ""), 10) : 0;
        return !!btn && amt > 0;
      });

      if (isEvent) await initEventPay();
      else await initOrderPay();
    } catch (e) {
      console.warn("[PAY] init skipped: total=0 or #payBtn missing", e?.message);
    }
  });

  /*** 주문 페이지 초기화 ***/
  async function initOrderPay() {
    const btn = document.getElementById("payBtn");
    if (!btn) return;

    const { amount, title, orderId } = extractOrderFromDom();
    if (!amount || amount <= 0) return;

    try {
      const res = await axiosInstance.post("/payment/session/start-order", null, {
        params: { orderId, amount, title }
      });
      if (res.data?.status !== "success") throw new Error(res.data?.message || "토큰 생성 실패");

      const { token, merchantUid } = res.data;
      hydrate(btn, token, merchantUid, amount, title);
      await syncContext(token);
      bindPayClick(btn);
    } catch (e) {
      console.error("[PAY:init order] ", e);
      alert(e.message || "결제 초기화 실패");
    }
  }

  /*** 이벤트 페이지 초기화 ***/
  async function initEventPay() {
    const btn = document.getElementById("payBtn");
    if (!btn) return;

    const { reservationToken, eventId, scheduleId, reservedCount, groupCounts, amount, title } = extractEventFromDom();
    if (!amount || amount <= 0) return;

    try {
      const res = await axiosInstance.post("/payment/session/start-event", null, {
        params: { reservationToken, eventId, scheduleId, reservedCount, amount, title, groupCounts }
      });
      if (res.data?.status !== "success") throw new Error(res.data?.message || "토큰 생성 실패");

      const { token, merchantUid } = res.data;
      hydrate(btn, token, merchantUid, amount, title);
      await syncContext(token);
      bindPayClick(btn);
    } catch (e) {
      console.error("[PAY:init event] ", e);
      alert(e.message || "결제 초기화 실패");
    }
  }

  /*** 공통: 버튼 데이터/hidden ***/
  function hydrate(btn, token, merchantUid, amount, title) {
    btn.dataset.token = token;
    btn.dataset.merchant = merchantUid;
    btn.dataset.amount = String(amount);
    btn.dataset.title = title || "BookAt 주문";

    const tokenInput = document.getElementById("pay_token");
    if (tokenInput) tokenInput.value = token;
  }

  /*** 공통: 세션 컨텍스트 ***/
  async function syncContext(token) {
    const ctx = await axiosInstance.get("/payment/session/context", { params: { token } }).then(r => r.data);
    if (ctx.status !== "success") throw new Error(ctx.message || "세션 동기화 실패");
  }

  /*** 공통: 결제 버튼 ***/
  function bindPayClick(btn) {
    if (btn.dataset.bound === "1") return;
    btn.dataset.bound = "1";

    ensureImpInit();

    btn.addEventListener("click", async () => {
      const IMP = window.IMP;
      if (!IMP || typeof IMP.request_pay !== "function") {
        alert("결제 모듈이 준비되지 않았습니다.");
        return;
      }

      const token = btn.dataset.token;
      const merchantUid = btn.dataset.merchant;
      const amount = Number(btn.dataset.amount || "0");
      const name = btn.dataset.title || "BookAt 결제";

      try {

        const rsp = await new Promise((resolve, reject) => {
          IMP.request_pay(
			// method 카드 고정
            { pg: "html5_inicis", pay_method: "card", merchant_uid: merchantUid, name, amount },
            (r) => {
              if (r?.success) return resolve(r);
              const err = new Error(r?.error_msg || "USER_CANCELED");
              err.code = r?.error_code || "USER_CANCELED";
              return reject(err);
            }
          );
        });

        const done = await axiosInstance.post("/payment/api/complete", { token, impUid: rsp.imp_uid });
        if (done.data?.status === "success" && done.data?.successRedirect) {
          window.location.href = done.data.successRedirect;
        } else {
          throw new Error(done.data?.message || "결제 검증 실패");
        }
      } catch (e) {
        console.error("[PAY] ", e);
        alert(e.message || "결제 처리 중 오류");
      }
    });
  }

  /*** DOM 주문 ***/
  function extractOrderFromDom() {
    // 1) 금액: #totalAmount 하나만 본다 (텍스트/값 둘 다 대응, 원/콤마 제거)
    const amount = parseAmountById("totalAmount");

    // 2) 제목
    const titles = (Array.isArray(window.orderItems) && window.orderItems.length)
      ? window.orderItems.map(it => (it.title || "").trim()).filter(Boolean)
      : Array.from(document.querySelectorAll(
          "[data-order-title], .book-title, .order-item .title, .order-item h3"
        ))
        .map(el => (el.textContent || "").trim())
        .filter(Boolean);

    let title = "BookAt 주문";
    if (titles.length >= 1) {
      const first = titles[0];
      const rest = titles.length - 1;
      title = rest > 0 ? `${first} 외 ${rest}권` : first;
    }

    //주문ID
    const idEl = document.querySelector("[data-order-id]");
    const orderIdRaw = idEl ? idEl.getAttribute("data-order-id") : null;
    const orderId = orderIdRaw && /^\d+$/.test(orderIdRaw) ? Number(orderIdRaw) : null;

    return { amount, title, orderId };
  }

  // --- 금액 파싱 (#id) ---
  function parseAmountById(id) {
    const el = document.getElementById(id);
    const raw = el ? (("value" in el ? el.value : el.textContent) || "0") : "0";
    return parseInt(String(raw).replace(/[^\d]/g, ""), 10) || 0;
  }

  /*** DOM 이벤트 ***/
  function extractEventFromDom() {
    const reservationToken = valueOf("#reservationToken");
    const eventId        = valueOf("#eventId");
    const scheduleId     = valueOf("#scheduleId");
    const reservedCount  = Number(valueOf("#reservedCount") || "1");
    const groupCounts    = valueOf("#groupCounts");

    const amtEl = document.getElementById("summaryTotal");
    const raw = amtEl ? (("value" in amtEl ? amtEl.value : amtEl.textContent) || "0") : "0";
    const amount = parseInt(String(raw).replace(/[^\d]/g, ""), 10) || 0;

    let title = valueOf("#eventName");

    return { reservationToken, eventId, scheduleId, reservedCount, groupCounts, amount, title };
  }

  function valueOf(sel) {
    const el = document.querySelector(sel);
    if (!el) return "";
    if ("value" in el) return el.value;
    return el.textContent?.trim() || "";
  }

})();

function wireTabs() {
  const tabs = document.querySelectorAll(".pay-method-btn");
  const card = document.getElementById("cardSection");
  const vbank = document.getElementById("vbankSection");

  // 초기 상태: 카드 활성화
  let activeMethod = "card";
  tabs.forEach(b => {
    if (b.classList.contains("is-active")) activeMethod = b.dataset.method || "card";
  });
  if (!tabs.length) return;

  const apply = (method) => {
    tabs.forEach(x => x.classList.toggle("is-active", x.dataset.method === method));
    if (card) card.style.display = method === "card" ? "block" : "none";
    if (vbank) vbank.style.display = method === "vbank" ? "block" : "none";
  };
  apply(activeMethod);

  tabs.forEach(btn => {
    btn.addEventListener("click", () => {
      activeMethod = btn.dataset.method || "card";
      apply(activeMethod);

    });
  });
}


