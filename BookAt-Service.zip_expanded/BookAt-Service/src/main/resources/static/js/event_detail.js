// 페이지의 모든 HTML 요소가 완전히 로드된 후 스크립트를 실행합니다.
document.addEventListener("DOMContentLoaded", function () {
  // 기능 1: 동적 예약 버튼 상태 업데이트 함수 호출
  initializeReserveButton();

  // 기능 2: 이벤트 위치를 표시하는 동적 지도 생성 함수 호출
  initializeLeafletMap();
});

/**
 * 예약 버튼의 상태를 날짜에 따라 동적으로 설정하는 함수
 */
function initializeReserveButton() {
  const reserveBtn = document.getElementById("reserve-btn");
  // 버튼이 없으면 오류 방지를 위해 함수를 종료합니다.
  if (!reserveBtn) {
    console.error("예약 버튼(id='reserve-btn')을 찾을 수 없습니다.");
    return;
  }

  const eventDateStr = reserveBtn.dataset.eventDate;
  if (!eventDateStr) {
    reserveBtn.textContent = "날짜 정보 없음";
    reserveBtn.disabled = true;
    return;
  }

  const now = new Date();
  now.setHours(0, 0, 0, 0);

  const eventDate = new Date(eventDateStr);
  eventDate.setHours(0, 0, 0, 0);

  const ticketingOpenDate = new Date(eventDate);
  ticketingOpenDate.setDate(ticketingOpenDate.getDate() - 30);
  ticketingOpenDate.setHours(18, 0, 0, 0); //예매일 당일 오픈 시간 설정하는 부분

  let countdownInterval;

  function updateButtonState() {
    const currentTime = new Date();

    if (now.getTime() >= eventDate.getTime()) {
      reserveBtn.textContent = "예매가 종료되었습니다";
      reserveBtn.disabled = true;
      if (countdownInterval) clearInterval(countdownInterval);
      return;
    }

    if (currentTime.getTime() >= ticketingOpenDate.getTime()) {
      reserveBtn.textContent = "예매하기";
      reserveBtn.disabled = false;
      reserveBtn.onclick = function () {
        alert("예매 페이지로 이동합니다.");
        // window.location.href = '/reservation-page';//여기에 url을 넣어버리면 사용자가 url보고 그냥 들어와버릴 수 있음... 컨트롤러에서 한번 더 막는 로직 필요
      };
      if (countdownInterval) clearInterval(countdownInterval);
      return;
    }

    const countdownStartDate = new Date(ticketingOpenDate);
    countdownStartDate.setDate(countdownStartDate.getDate() - 1);

    if (currentTime.getTime() >= countdownStartDate.getTime()) {
      reserveBtn.disabled = true;
      countdownInterval = setInterval(() => {
        const timeLeft = ticketingOpenDate.getTime() - new Date().getTime();

        if (timeLeft <= 0) {
          clearInterval(countdownInterval);
          updateButtonState();
          return;
        }

        const hours = Math.floor(timeLeft / (1000 * 60 * 60));
        const minutes = Math.floor((timeLeft % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((timeLeft % (1000 * 60)) / 1000);

        const formattedHours = String(hours).padStart(2, "0");
        const formattedMinutes = String(minutes).padStart(2, "0");
        const formattedSeconds = String(seconds).padStart(2, "0");

        reserveBtn.textContent = `예매 오픈까지 ${formattedHours}시간 ${formattedMinutes}분 ${formattedSeconds}초`;
      }, 1000);
    } else {
      const openDate = ticketingOpenDate.getDate();
      const openMonth = ticketingOpenDate.getMonth() + 1;
      const openYear = ticketingOpenDate.getFullYear();

      reserveBtn.textContent = `${openYear}년 ${openMonth}월 ${openDate}일 18:00 오픈예정`;
      reserveBtn.disabled = true;
    }
  }
  // 페이지 로드 시 버튼 상태를 즉시 업데이트합니다.
  updateButtonState();
}

// Leaflet 초기화
var map = L.map("map").setView({ lon: 127.766, lat: 36.355 }, 13);

// 최대 범위 지정
map.setMaxBounds([
  [32, 123],
  [44, 132.5],
]);

// '오픈스트리트맵 한국'에서 서비스하는 '군사 시설 없는 오픈스트리트맵 지도 타일'을 삽입
L.tileLayer("https://tiles.osm.kr/hot/{z}/{x}/{y}.png", {
  maxZoom: 19,
  attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap 기여자</a>',
}).addTo(map);

// 축척 막대를 지도 왼쪽 하단에 노출
L.control.scale({ imperial: true, metric: true }).addTo(map);

// 마커를 지도에 추가
L.marker({ lon: 127.766, lat: 36.355 }).bindPopup("대한민국의 중심지, 장연리마을").addTo(map);
