// 공통 로그아웃 처리
window.handleLogout = function () {
  axiosInstance.post("/user/logout")
    .then(() => {
      console.log("로그아웃 성공");
    })
    .catch(err => {
      console.warn("로그아웃 요청 중 에러:", err);
    })
    .finally(() => {
      if (window.clearRefreshTimer) {
        window.clearRefreshTimer();
      }
      localStorage.removeItem("accessToken");
      window.location.href = "/";
    });
};
