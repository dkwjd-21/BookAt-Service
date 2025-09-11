$(document).ready(async function() {
    const accessTokenKey = "accessToken";
	const ACCESS_TOKEN_REFRESH_THRESHOLD = 15000; // access token 만료 15초 전 갱신
	let refreshPromise = null;

	// 엑세스 토큰 만료 15초 전인지 판단
	function isTokenNearExpiry(token) {
	    if (!token) return true;
	    try {
			// header.payload.signature
			// payload.exp : 만료시간정보
	        const payload = JSON.parse(atob(token.split(".")[1]));
	        const exp = payload.exp * 1000;
			const timeLeft = exp - Date.now();
			console.log("access token 남은 시간(ms):", timeLeft);
	        return timeLeft < ACCESS_TOKEN_REFRESH_THRESHOLD; // 15초 전 갱신
	    } catch(e) {
	        return true;
	    }
	}

	// 엑세스 토큰이 만료됐는지 판단 (true: 만료, false: 유효)
    function isTokenExpired(token) {
        if (!token) return true;
        try {
            const payload = JSON.parse(atob(token.split(".")[1]));
            const exp = payload.exp * 1000;
            return Date.now() >= exp;
        } catch(e) {
            return true;
        }
    }

	async function handleLogout() {
        const token = localStorage.getItem(accessTokenKey);
		
		console.log("handleLogout 진입");
		console.log("로그아웃 요청 엑세스토큰 : " + token);

        try {
            await $.ajax({
                url: "/user/logout",
                type: "POST",
                headers: { "Authorization": "Bearer " + (token || "") },
                xhrFields: { withCredentials: true }
            });
        } catch(err) {
            console.error("서버 로그아웃 실패", err);
        } finally {
			localStorage.removeItem(accessTokenKey);
			window.location.href = "/";
		}
    }
	
	// 엑세스 토큰 만료 됐으면 리프레시토큰이 유효한 동안 갱신
	async function refreshAccessTokenIfNeeded() {
	    let token = localStorage.getItem(accessTokenKey);
	    console.log("현재 access token:", token);

	    if (!token || isTokenExpired(token)) {
	        if (!refreshPromise) {
	            refreshPromise = $.ajax({
	                url: "/auth/refresh",
	                type: "POST",
	                xhrFields: { withCredentials: true }
	            })
	            .done(res => {
	                if (res.accessToken) {
	                    localStorage.setItem(accessTokenKey, res.accessToken);
	                    token = res.accessToken;
	                }
	            })
	            .fail(async () => {
	                console.log("refresh token 만료, 자동 로그아웃 처리");
	                await handleLogout();
	            })
	            .always(() => { refreshPromise = null; });
	        }
	        await refreshPromise;
	    }

	    return token;
	}
	
	async function apiAjax(options) {
		let token = await refreshAccessTokenIfNeeded();
		if (!token) return;
		
		options.headers = options.headers || {};
		options.headers["Authorization"] = "Bearer " + token;
		try {
			return await $.ajax(options);
		} catch(err) {
			if (err.status === 401 && !options._retry) {
				options._retry = true;
				await refreshAccessTokenIfNeeded();
				return apiAjax(options);
			} else {
				throw err;
			}
		}
	}
	
    $("#loginBtn").click(() => window.location.href = "/user/login");
	$("#signupBtn").click(() => window.location.href = "/user/signup");

    $("#logoutBtn").click(async function(event) {
        event.preventDefault();

        await handleLogout();
    });
	
	async function loadMainPage() {
	    try {
	        const pageHtml = await apiAjax({
	            url: "/mainPage",
	            type: "GET"
	        });
	        $("body").html(pageHtml);
	        history.pushState(null, null, "/mainPage");
	    } catch (err) {
	        console.error("mainPage 로드 실패:", err);
	    }
	}
	
	//await loadMainPage();
});

