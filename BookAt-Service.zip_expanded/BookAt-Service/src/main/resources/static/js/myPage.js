// DOMContentLoaded 이벤트
document.addEventListener("DOMContentLoaded", function () {
  initMyPageNavigation();

  // 초기 로드 시 주문배송조회 데이터 로드
  const currentPath = window.location.pathname;
  if (currentPath === "/myPage" || currentPath === "/myPage/") {
    // orderList.js의 초기화 함수가 있다면 호출
    if (typeof window.initOrderList === "function") {
      window.initOrderList();
    }
  }
});

// 마이페이지 네비게이션 초기화
function initMyPageNavigation() {
  const navLinks = document.querySelectorAll(".mypage-nav__link");

  navLinks.forEach((link) => {
    link.addEventListener("click", function (e) {
      // 현재 페이지가 myPageMain인 경우에만 SPA 방식으로 처리
      const currentPath = window.location.pathname;
      if (currentPath === "/myPage" || currentPath === "/myPage/") {
        const href = this.getAttribute("href");

        // 주문배송조회와 예매내역만 SPA 방식으로 처리
        if (href.includes("/myPage/orderList")) {
          e.preventDefault();
          showFragment("order");
          updateActiveLink(this, navLinks);
        } else if (href.includes("/reservation")) {
          e.preventDefault();
          showFragment("reservation");
          updateActiveLink(this, navLinks);
        }
        // 다른 메뉴는 기본 동작 (페이지 이동)
      }
    });
  });
}

// active 링크 업데이트
function updateActiveLink(activeLink, allLinks) {
  allLinks.forEach((l) => l.classList.remove("is-active"));
  activeLink.classList.add("is-active");
}

// fragment 표시
function showFragment(fragmentType) {
  // 모든 fragment 숨기기
  const allFragments = document.querySelectorAll(".mypage-fragment");
  allFragments.forEach((fragment) => {
    fragment.classList.remove("active");
    fragment.style.display = "none";
  });

  // 선택된 fragment 보여주기
  const targetFragment = document.getElementById(`fragment-${fragmentType}`);
  if (targetFragment) {
    targetFragment.classList.add("active");
    targetFragment.style.display = "block";

    // fragment별 초기화 함수 호출
    if (fragmentType === "reservation") {
      goReservationDetails();
    } else if (fragmentType === "order") {
      // orderList.js의 초기화 함수가 있다면 호출
      if (typeof window.initOrderList === "function") {
        window.initOrderList();
      }
    }
  }
}

// 예매 내역 데이터 로드
async function goReservationDetails() {
  try {
    const res = await axiosInstance.get("/myPage/reservationDetails");
    const { status, reservations } = res.data;

    const reservationContainer = document.getElementById("reservation-container");
    const reservationMain = document.getElementById("reservation-main");
    const template = document.getElementById("reservation-template");

    if (!reservationContainer || !reservationMain || !template) {
      console.error("예매내역 컨테이너를 찾을 수 없습니다.");
      return;
    }

    reservationMain.innerHTML = "";

    if (reservations.length === 0) {
      reservationMain.innerHTML = '<p class="no-reservation-info">예매 내역이 없습니다.</p>';
      return;
    }

    reservations.forEach((reservation) => {
      const clone = template.cloneNode(true);

      const img = clone.querySelector(".event-post img");
      img.src = reservation.eventImg;

      clone.querySelector(".event-name").textContent = reservation.eventName;
      clone.querySelector(".schedule-info").textContent = formatDate(reservation.scheduleTime) + ` ( ${reservation.scheduleName} )`;
      clone.querySelector(".reserved-count").textContent = `${reservation.reservedCount}명`;
      clone.querySelector(".reservation-status").textContent = formatDate(reservation.reservationDate) + ` ( ${reservation.reservationStatus} )`;
      clone.querySelector(".total-price").textContent = `${reservation.totalPrice}원`;

      reservationMain.appendChild(clone);
    });
  } catch (err) {
    console.error("예매내역 로드 오류:", err);
    const reservationMain = document.getElementById("reservation-main");
    if (reservationMain) {
      reservationMain.innerHTML = '<p class="no-reservation-info">예매 내역을 불러오는데 실패했습니다.</p>';
    }
  }
}

// 날짜 포맷 변경
function formatDate(dateInfo) {
  const date = new Date(dateInfo);
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = date.getHours();
  const minute = date.getMinutes();

  return `${year}년 ${month}월 ${day}일 ${hour}시 ${minute}분`;
}
