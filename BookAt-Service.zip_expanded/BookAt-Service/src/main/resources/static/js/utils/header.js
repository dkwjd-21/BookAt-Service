document.addEventListener("DOMContentLoaded", () => {
	const loginBtn = document.getElementById('loginBtn');
	const signupBtn = document.getElementById('signupBtn');
	const logoutBtn = document.getElementById('logoutBtn');
	const myPageBtn = document.getElementById('myPageBtn');
	
	async function updateHeaderUI() {
		try {
			await window.validateUser(); // 로그인 여부 검증
			
			// 로그인 상태
			if (loginBtn) loginBtn.style.display = 'none';
			if (signupBtn) signupBtn.style.display = 'none';
			if (logoutBtn) logoutBtn.style.display = 'inline-block';
			if (myPageBtn) myPageBtn.style.display = 'inline-block';
		} catch(err) {
			// 비로그인 상태
			if (loginBtn) loginBtn.style.display = 'inline-block';
			if (signupBtn) signupBtn.style.display = 'inline-block';
			if (logoutBtn) logoutBtn.style.display = 'none';
			if (myPageBtn) myPageBtn.style.display = 'none';
		}
	}
	
	// 로그인
	if (loginBtn) {
	  loginBtn.addEventListener('click', (e) => {
		e.preventDefault();
	    window.location.href = '/user/login';
	  });
	}
	
	// 회원가입
	if (signupBtn) {
	  signupBtn.addEventListener('click', (e) => {
		e.preventDefault();
	    window.location.href = '/user/signup';
	  });
	}

	// 로그아웃
	if (logoutBtn) {
	  logoutBtn.addEventListener('click', () => {
		if (typeof window.handleLogout === 'function') {
			window.handleLogout();
		}
	  });
	}
	
	// 마이페이지로 이동
	if (myPageBtn) {
	  myPageBtn.addEventListener('click', () => {
		e.preventDefault();
		window.location.href = '/myPage';
	  });
	}
	
	updateHeaderUI();
});
