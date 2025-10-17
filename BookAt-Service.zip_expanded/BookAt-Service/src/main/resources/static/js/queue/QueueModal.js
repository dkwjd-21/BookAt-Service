let queueInterval = null;
let fetchAbortController = null;
let isFetching = false;

const EVENT_ID_KEY = "eventId";
const RESERVATION_ID_KEY = "reservationIdForModify";

// -------------------- 대기열 진입 --------------------
async function onModal(eventId) {
  try {
    await window.validateUser();
  } catch {
    alert("로그인한 회원님만 예매 가능합니다.");
    window.location.href = "/user/login";
    return;
  }

  const modal = document.querySelector(".queueModal-overlay");
  const mapOverlay = document.querySelector(".map-overlay");
  if (!modal) return;
  if (modal.style.display === "none") {
    modal.style.display = "flex";
    // 지도 오버레이 표시
    if (mapOverlay) {
      mapOverlay.style.display = "block";
    }
    if (!eventId) {
      console.error("이벤트 아이디가 전달되지 않았습니다.");
      modal.style.display = "none";
      // 지도 오버레이 숨김
      if (mapOverlay) {
        mapOverlay.style.display = "none";
      }
      return;
    }
    sessionStorage.setItem(EVENT_ID_KEY, eventId);

    try {
      const res = await axiosInstance.post("/queue/enter", null, {
        params: { eventId },
      });
      const data = res.data;
      if (data.status === "success") {
        const waitingNumEl = document.querySelector(".modal-waitingNum");
        if (waitingNumEl) waitingNumEl.textContent = data.rank ?? "-";
        startQueuePolling(true);
      } else {
        alert("대기열 진입 실패");
        modal.style.display = "none";
        // 지도 오버레이 숨김
        if (mapOverlay) {
          mapOverlay.style.display = "none";
        }
      }
    } catch (err) {
      console.error("대기열 진입 에러:", err);
      modal.style.display = "none";
      // 지도 오버레이 숨김
      if (mapOverlay) {
        mapOverlay.style.display = "none";
      }
    }
  }
}

// -------------------- 대기열 진입 (예약 변경 전용) --------------------
async function onModifyModal(eventId, reservationId) {
  try {
    await window.validateUser();
  } catch {
    alert("로그인한 회원님만 예약 변경 가능합니다.");
    window.location.href = "/user/login";
    return;
  }

  const modal = document.querySelector(".queueModal-overlay");
  const mapOverlay = document.querySelector(".map-overlay");
  if (!modal) return;
  if (modal.style.display === "none") {
    modal.style.display = "flex";
    // 지도 오버레이 표시
    if (mapOverlay) {
      mapOverlay.style.display = "block";
    }
    if (!eventId || !reservationId) {
      console.error("이벤트 아이디 또는 예약 아이디가 전달되지 않았습니다.");
      modal.style.display = "none";
      // 지도 오버레이 숨김
      if (mapOverlay) {
        mapOverlay.style.display = "none";
      }
      return;
    }
    // 예약 변경 로직에서는 eventId와 reservationId를 저장
    sessionStorage.setItem(EVENT_ID_KEY, eventId);
    sessionStorage.setItem(RESERVATION_ID_KEY, reservationId);

    try {
      // API 호출 시 reservationId를 함께 전달
      const res = await axiosInstance.post("/queue/enter", null, {
        params: { eventId, reservationId },
      });

      const data = res.data;
      if (data.status === "success") {
        const waitingNumEl = document.querySelector(".modal-waitingNum");
        if (waitingNumEl) waitingNumEl.textContent = data.rank ?? "-";
        // 폴링 시작
        startQueuePolling(true);
      } else {
        alert("대기열 진입 실패");
        modal.style.display = "none";
        // 지도 오버레이 숨김
        if (mapOverlay) {
          mapOverlay.style.display = "none";
        }
      }
    } catch (err) {
      console.error("대기열 진입 에러:", err);
      modal.style.display = "none";
      // 지도 오버레이 숨김
      if (mapOverlay) {
        mapOverlay.style.display = "none";
      }
    }
    console.log(`[예약 변경] 이벤트 아이디: ${eventId}, 예약 아이디: ${reservationId}`);
  }
}

// -------------------- 대기열 상태 폴링 --------------------
function startQueuePolling(immediate = false) {
  if (queueInterval !== null) return;
  if (immediate) performFetchAndUpdate();
  queueInterval = setInterval(() => performFetchAndUpdate(), 2000);
}

async function performFetchAndUpdate() {
  if (isFetching) return;
  isFetching = true;

  if (fetchAbortController) fetchAbortController.abort();
  fetchAbortController = new AbortController();
  const signal = fetchAbortController.signal;

  try {
    const eventId = encodeURIComponent(sessionStorage.getItem("eventId"));
    if (!eventId) return;

    const reservationId = sessionStorage.getItem(RESERVATION_ID_KEY);

    // heartbeat 보내기
    axiosInstance.post("/queue/heartbeat", null, { params: { eventId } });

    const res = await axiosInstance.get("/queue/status", {
      params: { eventId },
      signal,
    });
    const data = res.data;

    const waitingNumEl = document.querySelector(".modal-waitingNum");
    if (waitingNumEl) {
      const parsedRank = data.rank !== undefined && data.rank !== null ? Number(data.rank) : null;
      waitingNumEl.textContent = parsedRank !== null ? parsedRank : "-";
    }

    const waitingCountEl = document.querySelector(".modal-watingCount");
    if (waitingCountEl) waitingCountEl.textContent = data.waitingCount ?? "0";

    if (data.canEnter) {
      stopQueuePolling();
      closeModal();
      openReservationPopup(eventId, reservationId);
    }
  } catch (err) {
    if (err.name !== "AbortError") console.error("대기열 상태 확인 실패:", err);
  } finally {
    isFetching = false;
  }
}

function stopQueuePolling() {
  if (queueInterval !== null) clearInterval(queueInterval);
  queueInterval = null;
  if (fetchAbortController) fetchAbortController.abort();
  fetchAbortController = null;
  isFetching = false;
}

function closeModal() {
  const modal = document.querySelector(".queueModal-overlay");
  const mapOverlay = document.querySelector(".map-overlay");
  if (modal) modal.style.display = "none";
  // 지도 오버레이 숨김
  if (mapOverlay) {
    mapOverlay.style.display = "none";
  }
}

// -------------------- 대기열 탈퇴 --------------------
async function queueLeave(eventId, useBeacon = false) {
  if (!eventId) return;

  if (useBeacon && navigator.sendBeacon) {
    const payload = JSON.stringify({ eventId });
    const blob = new Blob([payload], { type: "application/json" });
    navigator.sendBeacon("/queue/leave", blob);
  } else {
    try {
      await axiosInstance.post("/queue/leave", null, { params: { eventId } });
    } catch (err) {
      console.error("대기열 제거 실패:", err);
    }
  }
}

// -------------------- 새로고침/브라우저 종료 시 탈퇴 --------------------
function leaveQueueOnUnload() {
  const eventId = sessionStorage.getItem("eventId");
  if (!eventId) return;

  // sendBeacon로 최대한 안전하게
  const payload = JSON.stringify({ eventId });
  const blob = new Blob([payload], { type: "application/json" });
  navigator.sendBeacon("/queue/leave", blob);
}

window.addEventListener("beforeunload", leaveQueueOnUnload);
window.addEventListener("unload", leaveQueueOnUnload);

// -------------------- 예약 팝업 --------------------
async function openReservationPopup(eventId, reservationId = null) {
  try {
    const params = { eventId };

    // 예매내역이 있으면 파라미터에 추가하여 API 호출
    if (reservationId) {
      params.reservationId = reservationId;
    }

    const popupRes = await axiosInstance.get("/reservation/start", {
      params,
      responseType: "text",
    });
    const popup = window.open("", "_blank", "width=1000,height=700");

    if (popup) {
      popup.document.write(popupRes.data);
      popup.document.close();
      await axiosInstance.post("/queue/enterActive", null, {
        params: { eventId },
      });
    } else {
      alert("팝업이 차단되었습니다. 브라우저 설정을 확인해주세요.");
    }
  } catch (err) {
    console.error("예약 팝업 열기 실패:", err);
    alert("예약 창을 열 수 없습니다. 로그인 상태를 확인하세요.");
  } finally {
    await queueLeave(eventId);
  }
}
