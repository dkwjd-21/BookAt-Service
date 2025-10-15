	window.accessTokenKey = 'accessToken';
	
	// axios 인스턴스 생성
	const axiosInstance = axios.create({
	  baseURL: window.location.origin,
	  withCredentials: true // refresh token이 HttpOnly 쿠키로 전달될 때 필요
	});
	
	// JWT 파싱 함수 (payload 추출)
	function parseJwt(token) {
		try {
			const base64Url = token.split('.')[1];
			const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
			const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
			return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
			}).join(''));
	    	return JSON.parse(jsonPayload);
		} catch (e) {
	    	return null;
	  	}
	}
	
	let refreshTimer = null;
	
	function clearRefreshTimer() {
		if (refreshTimer) {
			clearTimeout(refreshTimer);
			refreshTimer = null;
		}
	}
	window.clearRefreshTimer = clearRefreshTimer;
	
	// access token 만료 10전 재발급 시도
	function scheduleTokenRefresh(token) {
		// 이전에 걸려있던 타이머를 제거 (중복 예약 방지)
		clearRefreshTimer();

		// 토큰(JWT)을 디코딩해서 payload 부분(JSON)을 읽음
		const payload = parseJwt(token);
		if (!payload || !payload.exp) return;

		const exp = payload.exp * 1000;
		const now = Date.now();
		
		// 현재 시간과 비교해서 만료까지 남은 시간 계산
		// 만료 10초 전에 refresh 실행되도록 예약
		const refreshTime = exp - now - 10000;
	 
		// 이미 토큰이 거의 만료됐거나 만료됐다면 바로 갱신 시도
		if(refreshTime <= 0) {
			tryRefreshImmediately();
			return;
		}
	  
		console.log("곧 엑세스 토큰 재발급");
		
		// refreshTime 후에 tryRefreshImmediately() 실행되도록 예약
		refreshTimer = setTimeout(tryRefreshImmediately, refreshTime);
	}
	
	async function tryRefreshImmediately() {
		try {
			const res = await axiosInstance.post('/auth/refresh', {});
			const newToken = res.data.accessToken;
			if (!newToken) throw new Error("서버에서 access token 누락");
			
			localStorage.setItem(window.accessTokenKey, newToken);
			console.log('엑세스 토큰 갱신 성공');
			scheduleTokenRefresh(newToken);
			
			if (typeof window.updateAuthUI === 'function') {
				window.updateAuthUI();
			}
			
			// 대기 중 요청들에 새 토큰 전달
			onRefreshed(newToken);
		} catch(err) {
			console.error('토큰 자동 갱신 실패:', err);
			onRefreshed(null); // 대기열 정리
			if (typeof window.handleLogout === 'function') {
				window.handleLogout();
			}
		}
	}
	
	// 요청 인터셉터: 모든 요청에 access token 자동 첨부
	axiosInstance.interceptors.request.use(
		config => {
		    const token = localStorage.getItem(window.accessTokenKey);
		    if (token) {
				config.headers.Authorization = `Bearer ${token}`;
		    }
			return config;
		},
		error => Promise.reject(error)
	);
	
	// refresh 중복 요청 방지 및 대기열 처리
	// 여러 401이 동시에 떠도 실제 갱신은 1번만
	let isRefreshing = false;
	let refreshSubscribers = [];
	
	// 콜백 등록 함수
	// 여러 요청이 동시에 401 Unauthorized 로 실패했을 때, 한 요청만 실제로 refresh 토큰 요청을 보내고
	// 나머지 요청은 콜백 함수를 저장
	function subscribeTokenRefresh(cb) {
		refreshSubscribers.push(cb);
	}
	
	// 콜백 실행 함수
	// 실제로 새로운 토큰이 발급되거나(newToken), 실패해서 null 이 넘어오면
	// refreshSubscribers 배열에 쌓아둔 콜백들을 전부 실행
	// 배열을 비워서 다음 라운드를 준비
	function onRefreshed(newToken) {
		refreshSubscribers.forEach(cb => cb(newToken));
		refreshSubscribers = [];
	}
	
	// 응답 인터셉터: 401 → refresh 토큰으로 access token 재발급
	axiosInstance.interceptors.response.use(
		response => response,
		async error => {
			const originalRequest = error.config;
	
			if (error.response && error.response.status === 401 &&
				!originalRequest._retry && !originalRequest.url.includes('/auth/refresh')) {
				
				originalRequest._retry = true;

				if (!isRefreshing) {
					isRefreshing = true;
			        try {
						// refresh token으로 access token 재발급
						const res = await axiosInstance.post('/auth/refresh', {});
						const newToken = res.data.accessToken;
					  
						if (!newToken) throw new Error("서버에서 access token 누락");
			
						localStorage.setItem(window.accessTokenKey, newToken);
						scheduleTokenRefresh(newToken);
					  
						if (typeof window.updateAuthUI === 'function') {
							window.updateAuthUI();
						}
			
						// 대기 중 요청들에 새 토큰 전달
						onRefreshed(newToken);
			        } catch (refreshError) {
						// refresh 토큰 만료 → 자동 로그아웃
						if(refreshError.response && [401, 403].includes(refreshError.response.status)) {
							if(typeof window.handleLogout === 'function') {
								window.handleLogout();
							} else {
								clearRefreshTimer();
								localStorage.removeItem(window.accessTokenKey);
								window.location.href = '/';
							}
						}
	
						onRefreshed(null); // 대기열 정리
						return Promise.reject(refreshError);
		        	} finally {
						isRefreshing = false;
					}
				}

				// 새 토큰 발급될 때까지 다른 요청 대기 후 재시도
				return new Promise(resolve => {
					subscribeTokenRefresh(newToken => {
						if(newToken) {
							originalRequest.headers.Authorization = `Bearer ${newToken}`;
							resolve(axiosInstance(originalRequest));
						} else {
							resolve(Promise.reject(error));
						}
					});
      			});
    		}
			return Promise.reject(error);
		}
	);
	
	// 전역에서 사용 가능
	window.axiosInstance = axiosInstance;
