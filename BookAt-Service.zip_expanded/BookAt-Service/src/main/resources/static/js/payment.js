(function () {
  IMP.init("imp55525217");
  const PG = "html5_inicis";

  function setActiveMethod(method) {
    const hidden = document.getElementById("method");
    if (!hidden) return;

    const m = (method || "card").toLowerCase();
    hidden.value = m;

    document.querySelectorAll(".pay-method-btn").forEach(btn => {
      btn.classList.toggle("is-active", btn.dataset.method === m);
    });

    // vbank 영역 토글
    const vbank = document.getElementById("vbankSection");
    if (vbank) vbank.style.display = (m === "vbank") ? "block" : "none";
  }

  function triggerPay(merchantUid, amount, payMethod) {
    IMP.request_pay(
      {
        pg: PG,
        pay_method: payMethod, // "card" | "vbank"
        name: "북캣 결제",
        merchant_uid: merchantUid,
        amount: amount
      },
      function (rsp) {
        if (rsp.success) {
          document.getElementById("imp_uid").value = rsp.imp_uid;
          document.getElementById("payCompleteForm").submit();
        } else {
          alert("결제 실패: " + rsp.error_msg);
        }
      }
    );
  }

  // 초기화
  document.addEventListener("DOMContentLoaded", function () {
    const hidden = document.getElementById("method");
    setActiveMethod(hidden?.value || "card");

    // 현금영수증 체크 토글
    const chk = document.getElementById("cash_rcpt_check");
    const detail = document.getElementById("cash_rcpt_detail");
    if (chk && detail) {
      chk.addEventListener("change", () => {
        detail.style.display = chk.checked ? "block" : "none";
      });
    }

    // 라디오에 따라 입력 placeholder 교체
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
  });

  //결제수단 버튼 클릭 처리
  document.addEventListener("click", function (e) {
    const methodBtn = e.target.closest(".pay-method-btn");
    if (methodBtn) setActiveMethod(methodBtn.dataset.method);
  });

  //결제하기 버튼 클릭 처리
  document.addEventListener("click", function (e) {
    const btn = e.target.closest("#payBtn");
    if (!btn) return;

    //화면의 vbank 입력값들을 숨은 필드에 복사
    const depositor = document.getElementById("depositor_name_input")?.value?.trim() || "";
    const cashApply = document.getElementById("cash_rcpt_check")?.checked ? "Y" : "N";
    const cashType  = document.querySelector('input[name="cash_rcpt_type"]:checked')?.value || "DED";
    const cashNo    = document.getElementById("cash_rcpt_number_input")?.value?.trim() || "";

    const hDep  = document.getElementById("depositor_name_hidden");
    const hApp  = document.getElementById("cash_receipt_apply_hidden");
    const hType = document.getElementById("cash_receipt_type_hidden");
    const hNo   = document.getElementById("cash_receipt_number_hidden");

    if (hDep)  hDep.value  = depositor;
    if (hApp)  hApp.value  = cashApply;
    if (hType) hType.value = cashType;
    if (hNo)   hNo.value   = (cashApply === "Y" ? cashNo : "");

    //결제 트리거
    const merchantUid = btn.getAttribute("data-merchant");
    const amount = parseInt(btn.getAttribute("data-amount") || "0", 10);
    const payMethod = (document.getElementById("method")?.value || "card").toLowerCase();

    //동기화
    setActiveMethod(payMethod);

    triggerPay(merchantUid, amount, payMethod);
  });
})();