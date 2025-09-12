	// 엑세스토큰이 유효한지 검증 (사용자 로그인 여부)!	

	document.addEventListener('DOMContentLoaded', () => {
	  const loginBtn = document.getElementById('loginBtn');
	  const logoutBtn = document.getElementById('logoutBtn');
	  const signupBtn = document.getElementById('signupBtn');
	  
	  window.updateAuthUI = function () {

		  const accessToken = localStorage.getItem("accessToken");
		
		  if(accessToken) {
			axiosInstance.get('/auth/validate')
			  .then(res => {
			    console.log("현재 로그인 사용자:", res.data.userId);
			  
			  if (loginBtn) loginBtn.style.display = 'none';
			  if (signupBtn) signupBtn.style.display = 'none';
			  if (logoutBtn) logoutBtn.style.display = 'inline-block';
			  })
			  .catch(err => {
			    console.log("로그인 상태 아님:", err);
			  
			  if (loginBtn) loginBtn.style.display = 'inline-block';
			  if (signupBtn) signupBtn.style.display = 'inline-block';
			  if (logoutBtn) logoutBtn.style.display = 'none';
			  
			  localStorage.removeItem("accessToken");
			  });
		  } else {
			  if (loginBtn) loginBtn.style.display = 'inline-block';
			  if (signupBtn) signupBtn.style.display = 'inline-block';
			  if (logoutBtn) logoutBtn.style.display = 'none';
		  }
	  }
	  
	  window.handleLogout = function () {
		
		axiosInstance.post("/user/logout")
		.then(() => {
			console.log("로그아웃 성공");
		})
		.catch(err => {
		  console.warn("로그아웃 요청 중 에러:", err);
		})
		.finally(() => {
		  localStorage.removeItem("accessToken");
		  window.location.href = "/";
		});
	  }
	  
	    if (loginBtn) {
	      loginBtn.addEventListener('click', (e) => {
			e.preventDefault();
	        window.location.href = '/user/login';
	      });
	    }

		if (logoutBtn) {
		  logoutBtn.addEventListener('click', window.handleLogout);
		}

		window.updateAuthUI();
	});
