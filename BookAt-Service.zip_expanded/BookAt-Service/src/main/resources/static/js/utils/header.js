document.addEventListener("DOMContentLoaded", () => {
  const loginBtn = document.getElementById("loginBtn");
  const signupBtn = document.getElementById("signupBtn");
  const logoutBtn = document.getElementById("logoutBtn");
  const myPageBtn = document.getElementById("myPageBtn");

  async function updateHeaderUI() {
    try {
      await window.validateUser(); // 로그인 여부 검증

      // 로그인 상태
      if (loginBtn) loginBtn.style.display = "none";
      if (signupBtn) signupBtn.style.display = "none";
      if (logoutBtn) logoutBtn.style.display = "inline-block";
      if (myPageBtn) myPageBtn.style.display = "inline-block";
    } catch (err) {
      // 비로그인 상태
      if (loginBtn) loginBtn.style.display = "inline-block";
      if (signupBtn) signupBtn.style.display = "inline-block";
      if (logoutBtn) logoutBtn.style.display = "none";
      if (myPageBtn) myPageBtn.style.display = "none";
    }
  }

  // 로그인
  if (loginBtn) {
    loginBtn.addEventListener("click", (e) => {
      e.preventDefault();
      window.location.href = "/user/login";
    });
  }

  // 회원가입
  if (signupBtn) {
    signupBtn.addEventListener("click", (e) => {
      e.preventDefault();
      window.location.href = "/user/signup";
    });
  }

  // 로그아웃
  if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
      if (typeof window.handleLogout === "function") {
        window.handleLogout();
      }
    });
  }

  // 마이페이지로 이동
  if (myPageBtn) {
    myPageBtn.addEventListener("click", () => {
      e.preventDefault();
      window.location.href = "/myPage";
    });
  }

  updateHeaderUI();
});
// /event가 포함된 url일 때 이벤트 nav-item에 active 클래스 추가
document.addEventListener("DOMContentLoaded", function () {
  const navLinks = document.querySelectorAll(".nav-links .nav-item");
  const currentPath = window.location.pathname;

  if (navLinks && navLinks.length > 0) {
    navLinks.forEach((link) => {
      // 모든 nav-item에서 active 제거
      link.classList.remove("active");
      // /event가 포함된 url이면 이벤트 메뉴에 active 추가
      if (currentPath.includes("/events") && link.textContent.trim() === "이벤트") {
        link.classList.add("active");
      }
      // /books가 포함된 url이면 도서 메뉴에 active 추가 (기존 동작 보완)
      else if (currentPath.includes("/books") && link.textContent.trim() === "도서") {
        link.classList.add("active");
      }
    });
  }
});
