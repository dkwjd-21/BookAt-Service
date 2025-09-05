// auth.js
const accessTokenKey = "accessToken";
const ACCESS_TOKEN_REFRESH_THRESHOLD = 15000; // access token 만료 15초 전 갱신

/**
 * JWT payload 안전하게 파싱
 */
function parseJwt(token) {
    if (!token) return null;
    try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.warn("JWT 파싱 실패:", e);
        return null;
    }
}

/**
 * access token 남은 시간이 15초 미만인지 확인
 */
function isTokenNearExpiry(token) {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) return true;
    const timeLeft = payload.exp * 1000 - Date.now();
    return timeLeft < ACCESS_TOKEN_REFRESH_THRESHOLD;
}

/**
 * access token 만료 여부 확인
 */
function isTokenExpired(token) {
    const payload = parseJwt(token);
    if (!payload || !payload.exp) return true;
    return Date.now() >= payload.exp * 1000;
}

/**
 * 로그아웃 처리
 */
async function handleLogout() {
    const token = localStorage.getItem(accessTokenKey);

    console.log("handleLogout 진입");
    console.log("로그아웃 시점 엑세스 : " + token);

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

    localStorage.removeItem(accessTokenKey);
}

/**
 * access token 갱신 (필요 시만)
 */
async function refreshAccessTokenIfNeeded() {
    let token = localStorage.getItem(accessTokenKey);

    // 토큰 없거나 만료되었거나 15초 이하 남았으면 갱신 시도
    if (!token || isTokenNearExpiry(token)) {
        try {
            const res = await $.ajax({
                url: "/api/user/refresh",
                type: "POST",
                xhrFields: { withCredentials: true } // refresh token 쿠키 전송
            });

            if (res.accessToken) {
                localStorage.setItem(accessTokenKey, res.accessToken);
                token = res.accessToken;
            } else {
                // 발급 실패 시 로그아웃 처리
                await handleLogout();
                token = null;
            }
        } catch (err) {
            console.warn("Access token 재발급 실패 - refresh token 만료");
            await handleLogout();
            token = null;
        }
    }

    return token;
}

/**
 * 공통 ajax 래퍼
 * - access token 자동 첨부
 * - 만료 시 refresh 후 재시도
 */
async function ajaxWithRefresh(options) {
    let token = await refreshAccessTokenIfNeeded();

    if (!token) {
        return $.Deferred().reject("No access token").promise();
    }

    options = options || {};
    options.headers = options.headers || {};
    options.headers["Authorization"] = "Bearer " + token;
    options.xhrFields = { withCredentials: true };

    try {
        return await $.ajax(options);
    } catch (err) {
        // 만약 토큰 만료로 401 발생 시, refresh 후 재시도
        if (err.status === 401) {
            token = await refreshAccessTokenIfNeeded();
            if (token) {
                options.headers["Authorization"] = "Bearer " + token;
                return await $.ajax(options);
            }
        }
        throw err;
    }
}
