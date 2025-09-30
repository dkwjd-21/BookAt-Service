let queueInterval = null;
let fetchAbortController = null;
let isFetching = false;

// -------------------- 대기열 진입 --------------------
async function onModal() {
    try {
        await window.validateUser();
    } catch {
        alert("로그인한 회원님만 예매 가능합니다.");
        window.location.href = "/user/login";
        return;
    }

    const modal = document.querySelector(".queueModal-overlay");
    if (!modal) return;
    if (modal.style.display === "none") {
        modal.style.display = "flex";
        //const eventId = "115";
		const eventId = "100";
        sessionStorage.setItem("eventId", eventId);

        try {
            const res = await axiosInstance.post("/queue/enter", null, { params: { eventId } });
            const data = res.data;
            if (data.status === "success") {
                const waitingNumEl = document.querySelector(".modal-waitingNum");
                if (waitingNumEl) waitingNumEl.textContent = data.rank ?? "-";
                startQueuePolling(true);
            } else {
                alert("대기열 진입 실패");
                modal.style.display = "none";
            }
        } catch (err) {
            console.error("대기열 진입 에러:", err);
            modal.style.display = "none";
        }
        console.log("이벤트 아이디:", eventId);
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
		
		// heartbeat 보내기 
		axiosInstance.post("/queue/heartbeat", null, { params: { eventId } });

        const res = await axiosInstance.get("/queue/status", { params: { eventId }, signal });
        const data = res.data;

        const waitingNumEl = document.querySelector(".modal-waitingNum");
        if (waitingNumEl) {
            const parsedRank = (data.rank !== undefined && data.rank !== null) ? Number(data.rank) : null;
            waitingNumEl.textContent = parsedRank !== null ? parsedRank : "-";
        }

        const waitingCountEl = document.querySelector(".modal-watingCount");
        if (waitingCountEl) waitingCountEl.textContent = data.waitingCount ?? "0";

        if (data.canEnter) {
            stopQueuePolling();
            closeModal();
            openReservationPopup(eventId);
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
    if (modal) modal.style.display = "none";
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
async function openReservationPopup(eventId) {
    try {
        const popupRes = await axiosInstance.get("/reservation/start", { params: { eventId }, responseType: "text" });
        const popup = window.open("", "_blank", "width=1000,height=700");

        if (popup) {
            popup.document.write(popupRes.data);
            popup.document.close();
            await axiosInstance.post("/queue/enterActive", null, { params: { eventId } });
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


