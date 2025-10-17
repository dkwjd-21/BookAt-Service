// 본인인증 버튼 클릭
async function verification() {
  try {
    // 1. 포트원 본인인증을 호출 & 사용자가 완료할 때까지 기다림
    const portoneResponse = await PortOne.requestIdentityVerification({
      storeId: PORTONE_STORE_ID,
      // identityVerificationId: `identity-verification-${crypto.randomUUID()}`,
      // AWS 배포 환경에서 UUID 생성이 안되는 이슈 수정
      identityVerificationId: `identity-verification-${generateUUID()}`,
      channelKey: PORTONE_CHANNEL_KEY,
    });

    // 2. 사용자가 창을 닫거나 인증에 실패한 경우 에러를 발생시켜 중단
    if (portoneResponse.code != null) {
      throw new Error(portoneResponse.message);
    }

    // 3. 백엔드에 identityVerificationId를 보내 최종 검증을 요청
    const backendResponse = await fetch("/user/signup/verify", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        identityVerificationId: portoneResponse.identityVerificationId,
      }),
    });

    if (!backendResponse.ok) {
      throw new Error("서버에서 인증 정보를 확인하는 데 실패했습니다.");
    }

    // 4. [핵심] 백엔드가 보낸 JSON 응답을 객체로 변환
    const userInfo = await backendResponse.json();

    console.log(userInfo);

    // 5. 백엔드로부터 받은 상태가 'success'가 아니면 에러를 발생시켜 중단
    if (userInfo.status !== "success") {
      throw new Error(userInfo.message || "서버에서 인증 처리에 실패했습니다.");
    }

    // [로직추가] 이미 가입된 유저인지 확인
    const existsResponse = await fetch(
      `/user/signup/userExists?phone=${userInfo.phone}`
    );
    if (!existsResponse.ok) {
      throw new Error("서버에서 유저 존재 여부 확인 실패");
    }

    const exists = await existsResponse.json();
    if (exists) {
      alert("이미 가입된 사용자입니다. 로그인 페이지로 이동합니다.");
      window.location.href = "/user/login";
      return;
    }

    // 6. [성공] 모든 검증 완료 -> 화면 전환
    // 단계 표시 변경
    document.getElementById("step-fir").className = "step";
    document.getElementById("step-sec").className = "step-active";

    // 단계별 컨텐츠 변경
    document.querySelector(".signup-verification").style.display = "none";
    document.querySelector(".signup-form").style.display = "";

    console.log(userInfo);

    // 7. 가져온 이름과 전화번호를 폼에 채우기
    const userNameInput = document.getElementById("userName");
    if (userNameInput) {
      userNameInput.removeAttribute("readonly");

      userNameInput.value = userInfo.name;

      userNameInput.setAttribute("readonly", true);
    }

    const phoneInput = document.querySelector("input[name='phone']");
    if (phoneInput) {
      phoneInput.value = userInfo.phone;
    }

    const birthInput = document.querySelector("input[name = 'birth']");
    if (birthInput) {
      birthInput.value = userInfo.birth;
    }
  } catch (error) {
    // 모든 에러(인증 실패, 통신 오류 등)를 여기서 한 번에 처리
    alert(error.message);
  }
}

// 아이디 & 이메일 중복체크용 전역 변수
let isIdChecked = false;
let isEmailChecked = false;

// 아이디, 비밀번호, 이메일 유효성 체크용 전역 변수
let idValid = false;
let pwValid = false;
let emailValid = false;

// HTML 문서가 모두 로드되었을 때 스크립트 실행
document.addEventListener("DOMContentLoaded", function () {
  // #signup-form 태그 선택
  const signupForm = document.getElementById("signup-form");

  // 입력값 유효성 검사 추가
  const userIdInput = document.getElementById("userId");
  const userPwInput = document.getElementById("userPw");
  const emailInput = document.getElementById("email");

  const idMessage = document.getElementById("idMessage");
  const pwMessage = document.getElementById("pwMessage");
  const emailMessage = document.getElementById("emailMessage");

  // 아이디 검사
  userIdInput.addEventListener("input", function () {
    const userIdValue = userIdInput.value;

    // 아이디 입력 필드가 비어 있으면 메시지를 지운다.
    if (userIdValue.length === 0) {
      idMessage.textContent = "";
      return;
    }

    if (userIdValue.length >= 4) {
      idMessage.textContent = "";
      idValid = true;
    } else {
      idMessage.textContent = "아이디는 4자 이상이어야 합니다.";
      idValid = false;
    }
  });

  // 비밀번호 검사
  userPwInput.addEventListener("input", function () {
    const userPwValue = userPwInput.value;

    // 비밀번호 입력 필드가 비어 있으면 메시지를 지운다.
    if (userPwValue.length === 0) {
      pwMessage.textContent = "";
      return;
    }

    // 비밀번호 검사
    if (userPwValue.length >= 4) {
      pwMessage.textContent = "";
      pwValid = true;
    } else {
      pwMessage.textContent = "비밀번호는 4자 이상이어야 합니다.";
      pwValid = false;
    }
  });

  // 이메일 검사
  emailInput.addEventListener("input", function () {
    const emailValue = emailInput.value;

    // 아이디 입력 필드가 비어 있으면 메시지를 지웁니다.
    if (emailValue.length === 0) {
      emailMessage.textContent = "";
      return;
    }

    // 이메일 검사
    if (emailValue.length >= 4) {
      emailMessage.textContent = "";
      emailValid = true;
    } else {
      emailMessage.textContent = "이메일은 4자 이상이어야 합니다.";
      emailValid = false;
    }
  });

  // #signup-form에서 'submit' 이벤트가 발생했을 때
  signupForm.addEventListener("submit", function (event) {
    // submit 기본 이벤트 막기
    event.preventDefault();

    // form의 입력 요소
    const formData = new FormData(signupForm);
    const userData = {};

    formData.forEach((value, key) => {
      userData[key] = value;
    });

    console.log(userData);

    const userName = document.getElementById("userName");
    const agreeTerms = document.getElementById("agree-terms");
    const agreePrivacy = document.getElementById("agree-privacy");

    // 유효성 검사
    if (userData.userId === "" || userData.userId === null) {
      alert("아이디를 입력해 주세요.");
      document.getElementById("userId").focus();
      return;
    }
    if (!idValid) {
      alert("아이디는 4자 이상이어야 합니다.");
      return;
    }
    if (!isIdChecked) {
      alert("아이디 중복 검사를 완료해주세요.");
      return;
    }

    if (userData.userPw === "" || userData.userPw === null) {
      alert("비밀번호를 입력해 주세요.");
      document.getElementById("userPw").focus();
      return;
    }
    if (!pwValid) {
      alert("비밀번호는 4자 이상이어야 합니다.");
      return;
    }
    if (userData.email === "" || userData.email === null) {
      alert("이메일을 입력해 주세요.");
      document.getElementById("email").focus();
      return;
    }
    if (!emailValid) {
      alert("이메일은 4자 이상이어야 합니다.");
      return;
    }
    if (!isEmailChecked) {
      alert("이메일 중복 검사를 완료해주세요.");
      return;
    }

    if (!agreeTerms.checked) {
      alert("이용 약관에 동의해 주세요.");
      return;
    }
    if (!agreePrivacy.checked) {
      alert("개인정보처리방침에 동의해 주세요.");
      return;
    }

    // 유효성 검사 통과!
    // submit 기본 이벤트 실행 -> DB에 INSERT 요청
    // 서버로 전송할 가입정보 : userData
    fetch("/user/signup/insert", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(userData),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error("회원가입 실패 : 서버 응답 오류");
        }
        return response.json();
      })
      .then((data) => {
        console.log("회원가입 성공 : ", data);

        // 단계 표시 변경
        const stepFir = document.getElementById("step-fir");
        const stepSec = document.getElementById("step-sec");
        const stepThr = document.getElementById("step-thr");
        stepFir.className = "step";
        stepSec.className = "step";
        stepThr.className = "step-active";

        // 단계별 컨텐츠 변경
        const form = document.getElementsByClassName("signup-form")[0];
        const complete = document.getElementsByClassName("signup-complete")[0];
        form.style.display = "none";
        complete.style.display = "";
        document.getElementById("complete-username").textContent =
          data.userName;
      })
      .catch((error) => {
        console.error("회원가입 오류 : ", error);
        alert("회원가입 중 오류가 발생했습니다.");
      });
  });
});

// 아이디 중복확인
function chkId() {
  // 입력한 아이디 값
  const idVal = document.getElementById("userId").value;

  if (idVal === null || idVal === "") {
    alert("아이디를 입력해 주세요.");
    return;
  }

  // 서버로 중복 확인 요청 - 비동기식으로 진행
  fetch(`/user/signup/chkId?idVal=${idVal}`)
    .then((response) => {
      // 서버 응답이 성공적인지 확인
      if (!response.ok) {
        throw new Error("네트워크 응답이 올바르지 않습니다.");
      }
      // json 형식의 응답을 파싱
      return response.json();
    })
    .then((data) => {
      // 서버에서 받은 true/false 값 처리
      if (data === true) {
        console.log("사용 가능한 아이디 입니다.");
        confirm("사용 가능한 아이디 입니다.");
        isIdChecked = true;
        document.getElementById("userPw").disabled = false;
        document.getElementById("email").disabled = false;
      } else {
        confirm("이미 사용 중인 아이디 입니다.");
        console.log("이미 사용 중인 아이디입니다.");
      }
    })
    .catch((error) => {
      // 오류 발생 시 사용자에게 알리기
      console.error("요청 중 오류 발생 : " + error);
    });
}

// 이메일 중복확인
function chkEmail() {
  // 입력한 이메일 값
  const emailVal = document.getElementById("email").value;

  if (emailVal === null || emailVal === "") {
    alert("이메일을 입력해 주세요.");
    return;
  }

  // 서버로 중복 확인 요청 - 비동기식으로 진행
  fetch(`/user/signup/chkEmail?emailVal=${emailVal}`)
    .then((response) => {
      // 서버 응답이 성공적인지 확인
      if (!response.ok) {
        throw new Error("네트워크 응답이 올바르지 않습니다.");
      }
      // json 형식의 응답을 파싱
      return response.json();
    })
    .then((data) => {
      // 서버에서 받은 true/false 값 처리
      if (data === true) {
        console.log("사용 가능한 이메일 입니다.");
        confirm("사용 가능한 이메일 입니다.");
        isEmailChecked = true;
      } else {
        confirm("이미 사용 중인 이메일 입니다.");
        console.log("이미 사용 중인 이메일입니다.");
      }
    })
    .catch((error) => {
      // 오류 발생 시 사용자에게 알리기
      console.error("요청 중 오류 발생 : " + error);
    });
}

// 본인인증을 위한 UUID 생성 메서드
function generateUUID() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
