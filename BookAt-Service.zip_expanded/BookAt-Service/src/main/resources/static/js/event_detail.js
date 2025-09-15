// 페이지의 모든 HTML 요소가 완전히 로드된 후 모든 스크립트를 실행합니다.
document.addEventListener("DOMContentLoaded", function () {
  // 1. 리뷰 모달 기능 초기화
  initializeReviewModal();

  // 2. 예약 버튼 기능 초기화
  initializeReserveButton();

  // 3. 카카오맵 기능 초기화
  initializeKakaoMap();
});

/**
 * 리뷰 작성 모달의 이벤트를 처리하는 함수
 */
function initializeReviewModal() {
  // 모달 관련 요소 가져오기
  const openModalBtn = document.getElementById("openReviewModalBtn");
  const modal = document.getElementById("reviewModal");

  // 요소가 페이지에 없을 경우 오류를 방지하기 위해 존재 여부 확인
  if (!openModalBtn || !modal) {
    console.warn("리뷰 모달 관련 요소를 찾을 수 없습니다.");
    return;
  }

  const closeModalBtn = modal.querySelector(".close-btn");

  // 리뷰 작성 폼 관련 요소
  const reviewForm = document.getElementById("reviewForm");
  const stars = modal.querySelectorAll(".stars-input .star");
  const ratingInput = document.getElementById("rating");

  // '리뷰 작성' 버튼 클릭 시 모달 열기
  openModalBtn.addEventListener("click", () => {
    modal.style.display = "flex";
  });

  // 'X' 버튼 클릭 시 모달 닫기
  closeModalBtn.addEventListener("click", () => {
    modal.style.display = "none";
  });

  // 모달 바깥 영역 클릭 시 모달 닫기
  modal.addEventListener("click", (e) => {
    if (e.target === modal) {
      modal.style.display = "none";
    }
  });

  // --- 별점 로직 ---
  let currentRating = 0;

  stars.forEach((star) => {
    // 마우스 올렸을 때 임시로 별 채우기
    star.addEventListener("mouseover", () => {
      resetStarsVisual();
      const value = star.dataset.value;
      for (let i = 0; i < value; i++) {
        stars[i].style.color = "#f9d849";
      }
    });

    // 마우스 뗐을 때 선택된 별점으로 복원
    star.addEventListener("mouseout", resetStarsVisual);

    // 별 클릭 시 평점 고정
    star.addEventListener("click", () => {
      currentRating = star.dataset.value;
      ratingInput.value = currentRating; // 숨겨진 input에 값 설정
      resetStarsVisual();
    });
  });

  function resetStarsVisual() {
    stars.forEach((star, index) => {
      if (index < currentRating) {
        star.style.color = "#f9d849"; // 선택된 별점
      } else {
        star.style.color = "#ddd"; // 비선택 별점
      }
    });
  }

  // --- 폼 제출 로직 ---
  reviewForm.addEventListener("submit", function (e) {
    e.preventDefault(); // 기본 폼 제출 동작 방지

    // 폼 데이터 가져오기
    const formData = new FormData(reviewForm);
    const data = Object.fromEntries(formData.entries());

    // [주의!] URL을  Controller의 주소로 변경해야 합니다.	//미구현
    fetch("event/review", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("리뷰 등록에 실패했습니다.");
        }
        return response.json();
      })
      .then((result) => {
        alert("리뷰가 성공적으로 등록되었습니다!");
        modal.style.display = "none"; // 모달 닫기
        window.location.reload(); // 페이지 새로고침하여 새 리뷰 확인
      })
      .catch((error) => {
        console.error("Error:", error);
        alert(error.message);
      });
  });
}

/**
 * 예약 버튼의 상태를 날짜에 따라 동적으로 설정하는 함수
 */
function initializeReserveButton() {
  const reserveBtn = document.getElementById("reserve-btn");
  const eventId = reserveBtn.dataset.eventId;
  if (!reserveBtn) {
    console.error("예약 버튼(id='reserve-btn')을 찾을 수 없습니다.");
    return;
  }

  const eventDateStr = reserveBtn.dataset.eventDate;
  if (!eventDateStr) {
    reserveBtn.textContent = "날짜 정보 없음";
    reserveBtn.disabled = true;
    // [수정] 비활성화 시 버튼 색상 변경
    reserveBtn.style.backgroundColor = "#F0F0F0";
    return;
  }

  const now = new Date();
  now.setHours(0, 0, 0, 0);

  const eventDate = new Date(eventDateStr);
  eventDate.setHours(0, 0, 0, 0);

  const ticketingOpenDate = new Date(eventDate);
  ticketingOpenDate.setDate(ticketingOpenDate.getDate() - 30);
  ticketingOpenDate.setHours(18, 0, 0, 0); //예매일 당일 오픈 시간 설정

  let countdownInterval;

  function updateButtonState() {
    const currentTime = new Date(); // 1. 예매가 종료된 경우

    if (now.getTime() >= eventDate.getTime()) {
      reserveBtn.textContent = "예매가 종료되었습니다";
      reserveBtn.disabled = true;
      // [수정] 비활성화 시 버튼 색상 변경
      reserveBtn.style.backgroundColor = "#F0F0F0";
      if (countdownInterval) clearInterval(countdownInterval);
      return;
    } // 2. 예매가 가능한 경우

    if (currentTime.getTime() >= ticketingOpenDate.getTime()) {
      reserveBtn.textContent = "예매하기";
      reserveBtn.disabled = false;
      // [수정] 활성화 시 버튼 색상 변경
      reserveBtn.style.backgroundColor = "#f9d849";
      reserveBtn.onclick = function () {
        alert("예매 페이지로 이동합니다.");
        window.location.href = `/event/${eventId}/reservation`; // 예매 페이지 이동 로직
      };
      if (countdownInterval) clearInterval(countdownInterval);
      return;
    }

    const countdownStartDate = new Date(ticketingOpenDate);
    countdownStartDate.setDate(countdownStartDate.getDate() - 1);

    // 3. 카운트다운이 필요한 경우
    if (currentTime.getTime() >= countdownStartDate.getTime()) {
      reserveBtn.disabled = true;
      // [수정] 비활성화 시 버튼 색상 변경
      reserveBtn.style.backgroundColor = "#F0F0F0";
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
        reserveBtn.textContent = `예매 오픈 까지 ${String(hours).padStart(2, "0")}시간 ${String(minutes).padStart(2, "0")}분 ${String(seconds).padStart(2, "0")}초`;
      }, 1000);
    } else {
      // 4. 아직 오픈 일자가 많이 남은 경우
      const openDate = ticketingOpenDate.toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });
      reserveBtn.textContent = `${openDate} 18:00 오픈예정`;
      reserveBtn.disabled = true;
      // [수정] 비활성화 시 버튼 색상 변경
      reserveBtn.style.backgroundColor = "#F0F0F0";
    }
  } // 함수 최초 실행

  updateButtonState();
}

/**
 * eventAddress 변수를 기반으로 카카오맵을 생성하고 마커를 표시하는 함수
 */
function initializeKakaoMap() {
  if (typeof eventAddress === "undefined" || !eventAddress) {
    console.error("이벤트 주소(eventAddress)를 찾을 수 없습니다. HTML 파일에 inline script가 있는지 확인하세요.");
    return;
  }

  const mapContainer = document.getElementById("map");
  if (!mapContainer) {
    console.error("지도를 표시할 컨테이너(id='map')를 찾을 수 없습니다.");
    return;
  }

  const mapOption = {
    center: new kakao.maps.LatLng(37.566826, 126.9786567), // 기본 중심: 서울 시청
    level: 5,
  };
  const map = new kakao.maps.Map(mapContainer, mapOption);
  const ps = new kakao.maps.services.Places();

  ps.keywordSearch(eventAddress, function (data, status) {
    if (status === kakao.maps.services.Status.OK) {
      const firstResult = data[0];
      const coords = new kakao.maps.LatLng(firstResult.y, firstResult.x);

      const marker = new kakao.maps.Marker({
        map: map,
        position: coords,
      });

      const infowindow = new kakao.maps.InfoWindow({
        content: `<div style="padding:5px;font-size:12px;width:max-content;">${firstResult.place_name}</div>`,
      });
      infowindow.open(map, marker);

      map.setCenter(coords);
    } else {
      console.warn(`'${eventAddress}'에 대한 검색 결과가 없습니다.`);
      mapContainer.innerHTML = '<div style="display:flex; align-items:center; justify-content:center; height:100%;">장소 정보를 찾을 수 없습니다.</div>';
    }
  });
}
