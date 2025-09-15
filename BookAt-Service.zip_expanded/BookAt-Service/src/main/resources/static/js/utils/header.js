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
				const userInfo = res.data;
			    console.log("현재 로그인 사용자:", userInfo.userId);
			  
			  if (loginBtn) loginBtn.style.display = 'none';
			  if (signupBtn) signupBtn.style.display = 'none';
			  if (logoutBtn) logoutBtn.style.display = 'inline-block';
			  })
			  .catch(err => {
			    console.log("로그인 상태 아님:", err);
				
				if (typeof window.handleLogout === 'function') {
					window.handleLogout();
				} else {
					localStorage.removeItem("accessToken");
					window.location.href = "/user/login";
				}
			  });
		  } else {
			  if (loginBtn) loginBtn.style.display = 'inline-block';
			  if (signupBtn) signupBtn.style.display = 'inline-block';
			  if (logoutBtn) logoutBtn.style.display = 'none';
		  }
	  };
	  
	    if (loginBtn) {
	      loginBtn.addEventListener('click', (e) => {
			e.preventDefault();
	        window.location.href = '/user/login';
	      });
	    }

		if (logoutBtn) {
		  logoutBtn.addEventListener('click', () => {
			if (typeof window.handleLogout === 'function') {
				window.handleLogout();
			}
		  });
		}

		window.updateAuthUI();
	});
