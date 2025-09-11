// 현재 폴링 상태를 저장할 변수
// 폴링 : 일정한 주기 간격으로 계속 요청을 보내서, 새로운 데이터가 있는지 확인하는 방식
let queueInterval = null;

// 진행중인 fetch 요청을 취소할 수 있게 하는 컨트롤러
let fetchAbortController = null;

// 재진입 방지를 위한 플래그
let isFetching = false;

// 예매하기 버튼을 눌렀을 때 대기열 모달 띄우기
// 모달을 열면 대기열에 추가 -> 주기적으로 폴링 시작.
async function onModal() {
  const modal = document.getElementsByClassName("queueModal-overlay")[0];

  if (modal.style.display === "none") {
    // 모달 보여주기
    modal.style.display = "flex";
	
	// eventId & userId를 랜덤으로 생성
	const userId = generateRandomUserId();
	console.log("랜덤 유저 ID:", userId);
	const eventId = "21";
	
    try {
      // 먼저 큐에 진입 (서버에 유저 등록)
      const res = await fetch(`/queue/enter?eventId=${eventId}&userId=${encodeURIComponent(userId)}`, {
        method: "POST",
      });
      const data = await res.json();

      if (data.status === "success") {
        // 초기 대기 번호 표시
        const numberElement = document.querySelector(".modal-waitingNum");
        numberElement.textContent = data.rank ?? "-";

        // 2️⃣ 큐 상태 주기적으로 확인 시작
        startQueuePolling(true, userId);
      } else {
        alert("대기열 진입 실패");
        modal.style.display = "none";
      }
    } catch (err) {
      console.error("대기열 진입 에러:", err);
      modal.style.display = "none";
    }
  }
}

// immediate가 true면 setInterval을 기다리지 않고 즉시 fetch 1번 호출
// fetch가 끝나기 전에 같은 fetch가 중복 호출되는 걸 막기 위한 isFetching 플래그 사용
// 진행중인 fetch를 안전하게 취소하기 위해 AbortController 사용
function startQueuePolling(immediate = false, userId) {
  // 대기번호 요소
  const numberElement = document.getElementsByClassName("modal-waitingNum")[0];

  // 이미 실행중이면 중복 시작을 방지
  if (queueInterval !== null) {
    console.warn("폴링이 이미 실행 중입니다.");
    return;
  }

  // 즉시 실행 옵션 처리 (최초 1회)
  if (immediate) {
    performFetchAndUpdate(numberElement, userId);
  }

  // 2초 간격으로 performFetchAndUpdate 실행
  queueInterval = setInterval(() => performFetchAndUpdate(numberElement, userId), 2000);
}

// 실제 fetch 수행 함수
// isFetching 플래그로 중복 요청 방지
// AbortController를 새로 만들고 이전 컨트롤러는 필요시 취소
// 서버에서 오는 data.rank 타입 검사
async function performFetchAndUpdate(numberElement, userId) {
  // 이미 요청이 진행중이면 새로운 요청을 하지 않음 (중복 방지)
  if (isFetching) return;

  isFetching = true;

  // 기존에 만들어진 AbortController가 있으면(이전에 생성됐지만 아직 취소 안된 것) 취소
  if (fetchAbortController) {
    try {
      fetchAbortController.abort();
    } catch (e) {
    }
  }
  fetchAbortController = new AbortController();
  const signal = fetchAbortController.signal;

  try {
    // 실제로는 eventId, userId를 하드코딩 하지 말고 data- 속성 혹은 인증 토큰에서 가져와야 함
    const eventId = encodeURIComponent("21");

    // fetch에 signal을 넣으면 stopQueuePolling()에서 abort로 요청을 취소할 수 있음
    const res = await fetch(
      `/queue/status?eventId=${eventId}&userId=${encodeURIComponent(userId)}`,
      { signal }
    );

    // HTTP 응답 상태 체크
    if (!res.ok) {
      console.error("HTTP error:", res.status);
      isFetching = false;
      return;
    }

    const data = await res.json();

    if (data.status === "success") {
      // 서버에서 숫자나 null을 보내는 경우를 대비해 안전하게 파싱
      // - data.rank가 undefined/null이면 parsedRank는 null
      // - 숫자 혹은 문자열 숫자인 경우 Number()로 변환하고 NaN이면 null로 처리
      let parsedRank = null;
      if (data.rank !== undefined && data.rank !== null) {
        const num = Number(data.rank);
        parsedRank = Number.isFinite(num) ? num : null;
      }

      // 화면에 표시: 숫자가 있으면 숫자, 없으면 '-' 표시
      numberElement.textContent =
        parsedRank !== null ? String(parsedRank) : "-";

      // 사용자의 예약 가능 판단:
      // - parsedRank가 1보다 작거나 0이면 예약 가능으로 간주(서비스 규칙에 따라 조정)
      // - parsedRank === null 은 큐에 없음 또는 서버가 null 반환한 경우 (처리 정책에 따라 다른 동작 필요)
      if (parsedRank !== null && parsedRank <= 0) {
        stopQueuePolling();
        closeModal();
        // 실제 앱에서는 alert 보다는 사용자 흐름을 다음 화면으로 전환하거나 버튼 활성화 권장
        alert("예약 가능 상태가 되었습니다!");
      }
    } else {
      // 서버가 success가 아닌 경우(error 등)에 대한 로깅/처리
      console.error("서버 응답 에러:", data);
    }
  } catch (err) {
    // fetch가 abort되면 err.name === 'AbortError' 가 됨
    if (err.name === "AbortError") {
      console.info("요청이 취소되었습니다.");
    } else {
      console.error("대기열 상태 확인 실패:", err);
      // 네트워크 불안정 시 재시도 정책(지수 백오프 등)을 도입할 수 있음
    }
  } finally {
    // 요청 완료 또는 실패/취소 시 isFetching 플래그 해제
    isFetching = false;
  }
}

/**
 * 폴링 중지
 * - setInterval 제거
 * - 진행중인 fetch 요청이 있으면 Abort
 */
function stopQueuePolling() {
  if (queueInterval !== null) {
    clearInterval(queueInterval);
    queueInterval = null;
  }
  if (fetchAbortController) {
    try {
      fetchAbortController.abort();
    } catch (e) {
      /* 무시 */
    }
    fetchAbortController = null;
  }
  isFetching = false;
}

/**
 * 모달 닫기 (UI 제어)
 * - inline style을 직접 바꾸기보다 classList.toggle을 사용하면 CSS 제어가 쉬움
 */
function closeModal() {
  const modal = document.querySelector(".queueModal-overlay");
  modal.style.display = "none";
}

// 1부터 30까지 임의의 수를 지정하여 카운트다운 후 모달 없애기
function startCountdown() {
  const modal = document.getElementsByClassName("queueModal-overlay")[0];
  // 대기번호 요소
  const numberElement = document.getElementsByClassName("modal-waitingNum")[0];

  // 현재 대기 순번 저장
  let currentNumber = parseInt(numberElement.textContent);

  if (currentNumber > 0) {
    // 0.5초마다 반복 실행으로 대기 순번 앞당기기
    const interval = setInterval(function () {
      // 번호 앞당기기
      currentNumber -= 5;

      // 앞당긴 번호 화면에 표시하기
      numberElement.textContent = currentNumber;

      // 번호가 0 이하가 되면 멈춘다
      if (currentNumber <= 0) {
        clearInterval(interval);

        // 대기열 모달 화면에서 숨기기
        numberElement.textContent = 0;
        modal.style.display = "none";

        // 1부터 30까지 랜덤 수로 대기번호 초기화 (테스트용)
        const randomNumber = Math.floor(Math.random() * 30);
        numberElement.textContent = randomNumber;
      }
    }, 500);
  }
}

// 랜덤 유저 아이디 생성 함수
function generateRandomUserId() {
  const prefix = "user"; // 접두사
  const randomNum = Math.floor(Math.random() * 100000); // 0 ~ 99999
  return prefix + randomNum;
}
