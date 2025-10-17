// DOMContentLoaded 이벤트
document.addEventListener("DOMContentLoaded", function () {
  initMyPageNavigation();

  // 초기 로드 시 URL 기반 fragment 표시
  const currentPath = window.location.pathname;
  if (currentPath === "/myPage" || currentPath === "/myPage/") {
    // 사용자 정보 로드
    loadUserInfo();

    // URL 해시 또는 쿼리 파라미터 확인
    const hash = window.location.hash;
    const urlParams = new URLSearchParams(window.location.search);
    const fragment = urlParams.get("tab") || hash.replace("#", "");

    if (fragment === "reservation") {
      showFragment("reservation");
      updateActiveNavLink("reservation");
    } else if (fragment === "review") {
      showFragment("review");
      updateActiveNavLink("review");
    } else {
      // 기본값: 주문배송조회
      if (typeof window.initOrderList === "function") {
        window.initOrderList();
      }
      updateActiveNavLink("order");
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

        // 주문배송조회, 예매내역, 나의리뷰만 SPA 방식으로 처리
        if (href.includes("/myPage/orderList")) {
          e.preventDefault();
          showFragment("order");
          updateActiveLink(this, navLinks);
          updateURL("order");
        } else if (href.includes("/reservation")) {
          e.preventDefault();
          showFragment("reservation");
          updateActiveLink(this, navLinks);
          updateURL("reservation");
        } else if (href.includes("/myPage/myReview") || this.classList.contains("js-mypage-review-link")) {
          e.preventDefault();
          showFragment("review");
          updateActiveLink(this, navLinks);
          updateURL("review");
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

// 사용자 정보 동적 로드
async function loadUserInfo() {
  try {
    const response = await window.axiosInstance.get("/auth/validate");
    const userInfo = response.data;

    // 사용자 이름 업데이트
    const nameElement = document.querySelector(".mypage-profile__name strong");
    if (nameElement && userInfo.userName) {
      nameElement.textContent = userInfo.userName;
    }

    console.log("사용자 정보 로드 완료:", userInfo.userName);
  } catch (error) {
    console.log("사용자 정보 로드 실패:", error);
    // 사용자 정보가 없으면 빈 상태로 유지
  }
}

// 네비게이션 링크 활성 상태 업데이트 (fragment 기반)
function updateActiveNavLink(fragmentType) {
  const navLinks = document.querySelectorAll(".mypage-nav__link");

  // 모든 링크에서 is-active 클래스 제거
  navLinks.forEach((link) => link.classList.remove("is-active"));

  // 해당 fragment에 맞는 링크 찾아서 활성화
  let targetLink = null;

  if (fragmentType === "order") {
    targetLink = document.querySelector(".js-mypage-order-link");
  } else if (fragmentType === "reservation") {
    targetLink = document.querySelector('a[href*="/reservation"]');
  } else if (fragmentType === "review") {
    targetLink = document.querySelector(".js-mypage-review-link");
  }

  if (targetLink) {
    targetLink.classList.add("is-active");
  }
}

// URL 업데이트 (새로고침 시 상태 유지를 위해)
function updateURL(fragmentType) {
  const currentPath = window.location.pathname;
  if (currentPath === "/myPage" || currentPath === "/myPage/") {
    if (fragmentType === "order") {
      // 기본 상태이므로 쿼리 파라미터 제거
      window.history.replaceState({}, "", "/myPage");
    } else {
      // 다른 fragment는 쿼리 파라미터로 상태 저장
      window.history.replaceState({}, "", `/myPage?tab=${fragmentType}`);
    }
  }
}

// fragment 표시
function showFragment(fragmentType) {
  // 선택된 fragment 확인
  const targetFragment = document.getElementById(`fragment-${fragmentType}`);
  if (!targetFragment) return;

  // 이미 활성화되어 있는지 체크 (중복 초기화 방지)
  const wasActive = targetFragment.classList.contains("active");

  // 모든 fragment 숨기기
  const allFragments = document.querySelectorAll(".mypage-fragment");
  allFragments.forEach((fragment) => {
    fragment.classList.remove("active");
    fragment.style.display = "none";
  });

  // 선택된 fragment 보여주기
  targetFragment.classList.add("active");
  targetFragment.style.display = "block";

  // 이미 활성화되어 있지 않았다면 초기화 함수 호출
  if (!wasActive) {
    if (fragmentType === "reservation") {
      goReservationDetails();
    } else if (fragmentType === "order") {
      if (typeof window.initOrderList === "function") {
        window.initOrderList();
      }
    } else if (fragmentType === "review") {
      if (typeof window.initMyReview === "function") {
        window.initMyReview();
      }
    }
  }
}

// 예매 내역 데이터 로드
async function goReservationDetails() {
  try {
    const res = await window.axiosInstance.get("/myPage/reservationDetails");
    const { status, reservations } = res.data;

    const reservationContainer = document.getElementById("reservation-container");
    const reservationMain = document.getElementById("reservation-main");
    const template = document.getElementById("reservation-template");

    if (!reservationContainer || !reservationMain || !template) {
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
