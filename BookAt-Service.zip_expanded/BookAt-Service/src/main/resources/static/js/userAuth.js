	$(document).ready(async function() {
	    const accessTokenKey = "accessToken";
	
	    // Access Token 만료 여부 확인
	    function isTokenExpired(token) {
	        if (!token) return true;
	        try {
	            const payload = JSON.parse(atob(token.split(".")[1]));
	            const exp = payload.exp * 1000;
	            return Date.now() >= exp;
	        } catch (e) {
	            return true;
	        }
	    }
	
	    // 서버에 Access Token 검증 요청
	    async function validateAccessToken(token) {
	        if (!token) return false;
	        try {
	            await $.ajax({
	                url: "/auth/validate",
	                type: "POST",
	                headers: { "Authorization": "Bearer " + token }
	            });
	            console.log("Access Token 유효");
	            return true;
	        } catch (err) {
	            console.warn("Access Token 검증 실패:", err);
	            return false;
	        }
	    }
	
	    // Refresh Token으로 Access Token 재발급
	    async function refreshAccessToken() {
	        try {
	            const res = await $.ajax({
	                url: "/auth/refresh",
	                type: "POST",
	                xhrFields: { withCredentials: true } // Refresh Token 쿠키 전송
	            });
	            if (res.accessToken) {
	                localStorage.setItem(accessTokenKey, res.accessToken);
	                console.log("Access Token 재발급 성공:", res.accessToken);
	                return res.accessToken;
	            }
	            return null;
	        } catch (err) {
	            console.error("Refresh Token 만료 → 로그아웃 필요");
	            await handleLogout();
	            return null;
	        }
	    }
	
	    // 자동 인증 관리 함수
	    async function authenticate() {
	        let token = localStorage.getItem(accessTokenKey);
	
	        // Access Token 자체가 없으면 → Refresh 시도
	        if (!token) {
	            token = await refreshAccessToken();
	            return token;
	        }
	
	        // Access Token 만료 여부 확인
	        if (isTokenExpired(token)) {
	            console.log("Access Token 만료 → Refresh 시도");
	            token = await refreshAccessToken();
	            return token;
	        }
	
	        // Access Token이 아직 살아있으면 → 검증 API 호출
	        const valid = await validateAccessToken(token);
	        if (!valid) {
	            console.log("Access Token 유효하지 않음 → Refresh 시도");
	            token = await refreshAccessToken();
	        }
	
	        return token;
	    }
	
	    // 로그아웃 처리
	    async function handleLogout() {
	        const token = localStorage.getItem(accessTokenKey);
	        console.log("로그아웃 요청 Access Token:", token);
	
	        try {
	            await $.ajax({
	                url: "/user/logout",
	                type: "POST",
	                headers: { "Authorization": "Bearer " + (token || "") },
	                xhrFields: { withCredentials: true }
	            });
	        } catch (err) {
	            console.error("서버 로그아웃 실패:", err);
	        }
	
	        localStorage.removeItem(accessTokenKey);
	        window.location.href = "/user/login"; // 로그인 페이지로 이동
	    }
	
	    // 초기 실행: 인증 시도
	    await authenticate();

	});
