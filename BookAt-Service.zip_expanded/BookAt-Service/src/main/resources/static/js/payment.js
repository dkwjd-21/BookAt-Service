(function () {
  // 가맹점 식별코드
  IMP.init("imp55525217");

  const PG = "html5_inicis";   // PG사 고정

  function triggerPay(merchantUid, amount, payMethod) {
    IMP.request_pay({
      pg: PG,
      pay_method: payMethod,     // "card" | "vbank"
      name: "북캣 결제",
      merchant_uid: merchantUid,
      amount: amount
    }, function (rsp) {
      if (rsp.success) {
        document.getElementById("imp_uid").value = rsp.imp_uid;
        document.getElementById("payCompleteForm").submit();
      } else {
        alert("결제 실패: " + rsp.error_msg);
      }
    });
  }

  // 섹션/모달 어디든 payBtn 이 있으면 동작
  document.addEventListener("click", function (e) {
    const btn = e.target.closest("#payBtn");
    if (!btn) return;

    const merchantUid = btn.getAttribute("data-merchant");
    const amount = parseInt(btn.getAttribute("data-amount") || "0", 10);

    // 버튼 방식: hidden 필드에서 읽음 (없으면 card)
    const payMethodHidden = document.getElementById("method");
    const payMethod = (payMethodHidden?.value || "card").toLowerCase();

    triggerPay(merchantUid, amount, payMethod);
  });
})();