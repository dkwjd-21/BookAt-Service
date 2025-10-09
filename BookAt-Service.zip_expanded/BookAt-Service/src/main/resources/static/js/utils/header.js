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
    myPageBtn.addEventListener("click", async (e) => {
      e.preventDefault();
      try {
        const res = await window.axiosInstance.get("/order/orderList", {
          responseType: "text",
        });
        document.open();
        document.write(res.data);
        document.close();
        window.history.pushState({}, "", "/myPage/orderList");
      } catch (err) {
        console.log("로그인이 필요함");
        window.location.href = "/user/login";
      }
    });
  }

  updateHeaderUI();
});
// /event가 포함된 url일 때 이벤트 nav-item에 active 클래스 추가, 아니면 도서에 active 추가
document.addEventListener("DOMContentLoaded", function () {
  const navLinks = document.querySelectorAll(".nav-links .nav-item");
  const currentPath = window.location.pathname;

  if (navLinks && navLinks.length > 0) {
    navLinks.forEach((link) => {
      // 모든 nav-item에서 active 제거
      link.classList.remove("active");

      // /event가 포함된 url이면 이벤트 메뉴에 active 추가
      if (currentPath.includes("/event") && link.textContent.trim() === "이벤트") {
        link.classList.add("active");
      }
      // /event가 포함되지 않은 url이면 도서 메뉴에 active 추가
      else if (!currentPath.includes("/event") && link.textContent.trim() === "도서") {
        link.classList.add("active");
      }
    });
  }
});
