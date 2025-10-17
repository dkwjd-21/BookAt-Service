
(async function () {
  const wrap = document.getElementById("payList");
  const countLabel = document.getElementById("countLabel");

  const fmt = (n) => new Intl.NumberFormat().format(n || 0);

  function render(list) {
    countLabel.textContent = `${list.length}건`;
    wrap.innerHTML = "";

    list.forEach((p) => {
      const li = document.createElement("li");
      li.className = "pay-item";

      const statusText =
        p.paymentStatus === 1 ? "결제완료"
      : (p.paymentStatus === 2 || p.paymentStatus === -2) ? "취소"
      : (p.paymentStatus === -1) ? "실패"
      : "대기";

      li.innerHTML = `
        <div class="pay-meta">
          <div class="pay-title">${p.paymentInfo || "도서 결제"}</div>
          <div class="pay-sub muted">
            <span>금액</span> <strong>${fmt(p.paymentPrice)}원</strong>
            <span class="sep">·</span>
            <span>일시</span> <strong>${(p.paymentDate || "").replace("T"," ").slice(0,19)}</strong>
            <span class="sep">·</span>
            <span>상태</span> <span class="status-badge">${statusText}</span>
          </div>
          <div class="pay-ids">
            <span class="muted">MID:</span> <code>${p.merchantUid || "-"}</code>
            <span class="muted">PG:</span> <code>${p.pgTid || "-"}</code>
          </div>
        </div>
        <div class="pay-actions">
          ${
            p.paymentStatus === 1
              ? `<button class="btn btn--danger" data-merchant="${p.merchantUid}">결제 취소</button>`
              : ``
          }
          <a class="btn" href="${p.receiptUrl || "#"}" target="_blank" ${p.receiptUrl ? "" : "aria-disabled='true'"}>영수증</a>
        </div>
      `;
      wrap.appendChild(li);
    });
  }

  try {
    // 1) 바로 내 결제 목록 호출 → 여기서 로그인 여부를 판단
    const res = await axiosInstance.get("/payment/api/my").catch(err => err.response);

    // (미인증) 401/403 또는 바디가 실패일 때 → 로그인 페이지로 이동
    if (!res || res.status !== 200 || res.data?.status !== "success") {
      window.location.href = "/user/login";
      return;
    }

    // (정상) 리스트 렌더
    const list = Array.isArray(res.data.data) ? res.data.data : [];
    render(list);

    // 2) 결제 취소 버튼(위임)
    wrap.addEventListener("click", async (e) => {
      const btn = e.target.closest(".btn.btn--danger");
      if (!btn) return;

      const merchantUid = btn.dataset.merchant;
      if (!merchantUid) return;

      if (!confirm("해당 결제를 취소할까요?")) return;

      try {
        const r = await axiosInstance.post("/payment/api/cancel", { merchantUid }).catch(err => err.response);
        if (r && r.status === 200 && r.data?.status === "success") {
          alert("취소가 완료되었습니다.");
          location.reload();
          return;
        }
        // 미인증/세션만료 → 로그인 페이지로
        if (!r || r.status === 401 || r.status === 403 || r.data?.message === "unauthorized") {
          alert("다시 로그인해 주세요.");
          window.location.href = "/user/login";
          return;
        }
        // 기타 실패
        alert(r?.data?.message || "취소 실패");
      } catch (err) {
        alert(err?.response?.data?.message || err?.message || "취소 중 오류");
      }
    });
  } catch (e) {
    console.error(e);
    alert("페이지를 불러오는 중 오류가 발생했습니다.");
  }
})();
