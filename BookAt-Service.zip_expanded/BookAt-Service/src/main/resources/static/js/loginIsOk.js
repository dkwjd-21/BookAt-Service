$(document).ready(async function() {
    const accessTokenKey = "accessToken";
	
	const ACCESS_TOKEN_REFRESH_THRESHOLD = 15000; // access token 만료 15초 전 갱신

	// 엑세스 토큰 만료 15초 전인지 판단
	function isTokenNearExpiry(token) {
	    if (!token) return true;
	    try {
	        const payload = JSON.parse(atob(token.split(".")[1]));
	        const exp = payload.exp * 1000;
			const timeLeft = exp - Date.now();
			console.log("Access token 남은 시간(ms):", timeLeft);
	        return timeLeft < ACCESS_TOKEN_REFRESH_THRESHOLD; // 15초 전 갱신
	    } catch(e) {
	        return true;
	    }
	}

	// 엑세스 토큰이 만료됐는지 판단
    function isTokenExpired(token) {
        if (!token) return true;
        try {
            const payload = JSON.parse(atob(token.split(".")[1]));
            const exp = payload.exp * 1000;
            return Date.now() >= exp;
        } catch(e) {
            return true; // 파싱 실패하면 만료로 간주
        }
    }

	async function handleLogout() {
        const token = localStorage.getItem(accessTokenKey);
		
		console.log("handleLogout 진입");
		console.log("로그아웃 시점 엑세스 : " + token);

        // 백엔드 로그아웃 요청
        try {
            await $.ajax({
                url: "/api/user/logout",
                type: "POST",
                headers: { "Authorization": "Bearer " + (token || "") },
                xhrFields: { withCredentials: true }
            });
        } catch(err) {
            console.error("서버 로그아웃 실패", err);
        }

        // 프론트 토큰 삭제 & UI 갱신
        localStorage.removeItem(accessTokenKey);
        updateAuthUI(null);
    }
	
    async function refreshAccessTokenIfNeeded() {
        let token = localStorage.getItem(accessTokenKey);
        console.log("현재 access token:", token);

//        if (!token || isTokenNearExpiry(token)) {
		if (!token || isTokenExpired(token)) {
            try {
                const res = await $.ajax({
                    url: "/api/user/refresh",
                    type: "POST",
                    xhrFields: { withCredentials: true } // HttpOnly 쿠키 전송
                });
                console.log("new access token:", res.accessToken);
                if (res.accessToken) {
                    localStorage.setItem(accessTokenKey, res.accessToken);
                    token = res.accessToken;
                }
            } catch(err) {
                console.log("자동 로그인 실패, access token 발급 불가");
				
				await handleLogout();
                token = null;
            }
        }

        return token;
    }

    const token = await refreshAccessTokenIfNeeded();
    updateAuthUI(token);

    function updateAuthUI(token) {
        if(token){
            $("#loginBtn").hide();
            $("#logoutBtn").show();
        } else {
            $("#loginBtn").show();
            $("#logoutBtn").hide();
        }
    }

    $("#loginBtn").click(() => window.location.href = "/api/user/login");

    $("#logoutBtn").click(async function(event) {
        event.preventDefault();

        await handleLogout();
    });
});

