
(function () {


  // IMP 초기화
  IMP.init("imp55525217");
  const PG = "html5_inicis";

  async function ensureAuth() {
    const ok = await validateUser();
    if (!ok) throw new Error("unauthorized");
  }

  function setActiveMethod(method) {
    const hidden = document.getElementById("method");
    if (!hidden) return;
    const m = (method || "card").toLowerCase();
    hidden.value = m;

    document.querySelectorAll(".pay-method-btn").forEach(btn => {
      btn.classList.toggle("is-active", btn.dataset.method === m);
    });

    const vbank = document.getElementById("vbankSection");
    if (vbank) vbank.classList.toggle("open", m === "vbank");
  }

  // --- 1) 진입: 세션 컨텍스트를 API로 로드해서 화면에 바인딩 ---
  document.addEventListener("DOMContentLoaded", async () => {
    const token = new URLSearchParams(location.search).get("token");
    if (!token) return; 

    try {
      await ensureAuth();
      const res = await axiosInstance.get("/payment/session/context", { params: { token } });
      if (res.data?.status !== "success") {
        alert(res.data?.message || "세션을 불러오지 못했습니다.");
        return;
      }
      const ctx = res.data;

      const methodHidden = document.getElementById("method");
      if (methodHidden) methodHidden.value = (ctx.method || "card").toLowerCase();
      setActiveMethod(methodHidden?.value || "card");

      const itemName = document.getElementById("item_name");
      if (itemName) itemName.value = ctx.title;

      const payBtn = document.getElementById("payBtn");
      if (payBtn) {
        payBtn.dataset.merchant = ctx.merchantUid;
        payBtn.dataset.amount = String(ctx.amount);
        payBtn.dataset.title = ctx.title;
      }


      const chk = document.getElementById("cash_rcpt_check");
      const detail = document.getElementById("cash_rcpt_detail");
      if (chk && detail) {
        chk.addEventListener("change", () => {
          detail.classList.toggle("open", chk.checked);
        });
      }
      const radios = document.querySelectorAll('input[name="cash_rcpt_type"]');
      const numberInput = document.getElementById("cash_rcpt_number_input");
      const updatePlaceholder = () => {
        const type = document.querySelector('input[name="cash_rcpt_type"]:checked')?.value || "DED";
        if (numberInput) {
          numberInput.placeholder = (type === "BIZ") ? "사업자번호 입력" : "휴대폰번호 입력";
        }
      };
      radios.forEach(r => r.addEventListener("change", updatePlaceholder));
      updatePlaceholder();

    } catch (e) {
      console.error(e);
      alert("인증이 만료되었거나 접근 권한이 없습니다.");
    }
  });


  document.addEventListener("click", (e) => {
    const methodBtn = e.target.closest(".pay-method-btn");
    if (methodBtn) setActiveMethod(methodBtn.dataset.method);
  });

  // --- 3) 결제 트리거: IMP 결제 → 완료 API 호출 → 성공 URL로 이동 ---
  async function completePaymentViaApi(rsp, fallbackMerchantUid) {
    const token = new URLSearchParams(location.search).get("token");

    const depositor = document.getElementById("depositor_name_input")?.value?.trim() || "";
    const apply = document.getElementById("cash_rcpt_check")?.checked ? "Y" : "N";
    const type  = document.querySelector('input[name="cash_rcpt_type"]:checked')?.value || "DED";
    const number = document.getElementById("cash_rcpt_number_input")?.value?.trim() || "";

    await ensureAuth();

    const body = {
      token,
      impUid: rsp.imp_uid,
      merchantUid: rsp.merchant_uid || fallbackMerchantUid,
      depositor,
      cashReceiptApply: apply,
      cashReceiptType: apply === "Y" ? type : null,
      cashReceiptNumber: apply === "Y" ? number : null
    };

    const res = await axiosInstance.post("/payment/api/complete", body);
    if (res.data?.status === "success") {
      window.location.href = res.data.successRedirect; 
      throw new Error(res.data?.message || "결제 검증 실패");
    }
  }

  function triggerPay(merchantUid, amount, payMethod) {
    IMP.request_pay({
      pg: PG,
      pay_method: payMethod,
      name: document.getElementById("item_name")?.value || "상품명",
      merchant_uid: merchantUid,
      amount: amount
    }, (rsp) => {
      if (!rsp.success) {
        alert(rsp.error_msg || "결제 실패");
        return;
      }
      completePaymentViaApi(rsp, merchantUid).catch(err => {
        console.error(err);
        alert(err.message || "결제 완료 처리 중 오류");
      });
    });
  }

  document.addEventListener("click", (e) => {
    const btn = e.target.closest("#payBtn");
    if (!btn) return;

    const payMethod = (document.getElementById("method")?.value || "card").toLowerCase();
    const merchantUid = btn.getAttribute("data-merchant");
    const amount = parseInt(btn.getAttribute("data-amount") || "0", 10);


    triggerPay(merchantUid, amount, payMethod);
  });

})();
