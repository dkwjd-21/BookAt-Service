$(document).ready(async function() {
    const accessTokenKey = "accessToken";
	
	const ACCESS_TOKEN_REFRESH_THRESHOLD = 15000; // access token 만료 15초 전 갱신

	// 엑세스 토큰 만료 15초 전인지 판단
	function isTokenNearExpiry(token) {
	    if (!token) return true;
	    try {
			// header.payload.signature
			// payload.exp : 만료시간정보
	        const payload = JSON.parse(atob(token.split(".")[1]));
	        const exp = payload.exp * 1000;
			const timeLeft = exp - Date.now();
			console.log("Access token 남은 시간(ms):", timeLeft);
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
        }
		
        localStorage.removeItem(accessTokenKey);
		$("#loginBtn, #signupBtn").show();
		$("#logoutBtn").hide();
    }
	
	// 엑세스 토큰 만료 됐으면 리프레시토큰이 유효한 동안 갱신
    async function refreshAccessTokenIfNeeded() {
        let token = localStorage.getItem(accessTokenKey);
        console.log("현재 access token:", token);

//        if (!token || isTokenNearExpiry(token)) {
		if (!token || isTokenExpired(token)) {
            try {
                const res = await $.ajax({
                    url: "/auth/refresh",
                    type: "POST",
                    xhrFields: { withCredentials: true } // HttpOnly 쿠키 전송
                });
                console.log("new access token:", res.accessToken);
                if (res.accessToken) {
                    localStorage.setItem(accessTokenKey, res.accessToken);
                    token = res.accessToken;
                }
            } catch(err) {
                console.log("refresh 토큰 만료, 로그인 실패, access token 발급 불가");
				// 리프레시 토큰 만료되면 자동 로그아웃
				await handleLogout();
                token = null;
            }
        }

        return token;
    }

    async function updateAuthUI() {
		const token = await refreshAccessTokenIfNeeded();
		
        if(token) {
			try {
                const res = await $.ajax({
                    url: "/auth/validate",
                    type: "POST",
                    headers: { "Authorization": "Bearer " + token }
                });
                console.log("토큰 검증 성공:", res);
                $("#loginBtn, #signupBtn").hide();
                $("#logoutBtn").show();
            } catch (xhr) {
				console.warn("토큰 검증 실패:", xhr.responseText);
				$("#loginBtn, #signupBtn").show();
				$("#logoutBtn").hide();
			}
        } else {
            $("#loginBtn, #signupBtn").show();
            $("#logoutBtn").hide();
        }
    }

	await updateAuthUI();
	
    $("#loginBtn").click(() => window.location.href = "/user/login");
	$("#signupBtn").click(() => window.location.href = "/user/signup");

    $("#logoutBtn").click(async function(event) {
        event.preventDefault();

        await handleLogout();
    });
});

