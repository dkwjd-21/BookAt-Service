// 엑세스토큰이 유효한지 검증 (사용자 로그인 여부)! -> 헤더가 들어가지 않는 모달창이나 팝업창에 사용되는 용도

document.addEventListener('DOMContentLoaded', () => {
  
  window.validateUser = async function () {

	  const accessToken = localStorage.getItem("accessToken");
	  
	  if(!accessToken) {
		console.log("access token 없음, 비로그인 상태");
		window.location.href = "/";
		return;
	  }
	  
	  try {
		const res = await axiosInstance.get("/auth/validate");
		const userInfo = res.data;
		console.log("현재 로그인 사용자 : ", userInfo.userId);
	  } catch(err) {
		console.log("로그인 상태 아님 : ", err);
		localStorage.removeItem("accessToken");
		window.location.href = "/";
	  }
	};

	window.validateUser();
});
