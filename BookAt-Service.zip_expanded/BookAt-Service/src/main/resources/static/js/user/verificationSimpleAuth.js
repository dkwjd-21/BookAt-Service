// 본인인증을 위한 UUID 생성 메서드 (AWS 호환성)
function generateUUID() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

async function verificationFindIdPw(storeId, channelKey, onSuccess, onError) {
  try {
    // 1. 포트원 본인인증 호출
    const portoneResponse = await PortOne.requestIdentityVerification({
      storeId: storeId,
      identityVerificationId: `identity-verification-${generateUUID()}`,
      channelKey: channelKey,
    });

    if (portoneResponse.code != null) {
      throw new Error(portoneResponse.message);
    }

    // 2. 백엔드 검증 요청
    const backendResponse = await fetch("/user/verify", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        identityVerificationId: portoneResponse.identityVerificationId,
      }),
    });

    if (!backendResponse.ok) {
      throw new Error("서버에서 인증 정보를 확인하는 데 실패했습니다.");
    }

    const userInfo = await backendResponse.json();

    if (userInfo.status === "success") {
      if (onSuccess) onSuccess(userInfo);
    } else {
      if (onError) onError(userInfo);
    }
  } catch (error) {
    alert(error.message);
  }
}
