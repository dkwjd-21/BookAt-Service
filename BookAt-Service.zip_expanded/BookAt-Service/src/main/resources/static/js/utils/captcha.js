
document.addEventListener("DOMContentLoaded", function() {
});

// redis key 저장 전역 변수 : 전역으로 captchaId 관리
let currentCaptchaId = null;

// 캡챠 모달 열기 함수 (콜백 받아서 저장)
function showCaptchaModal() {
	const modal = document.getElementById("captchaModal");
	modal.style.display = "block";
	refreshImage();
}

// 캡챠 모달을 숨기는 함수
function hideCaptchaModal() {
    const modal = document.getElementById('captchaModal');
    modal.style.display='none';
}

// 이미지 새로고침
async function refreshImage() {
	const captchaImage = document.getElementById('captchaImg');
	
	try {
		const res = await axiosInstance.get("/api/captcha/image");
		// redis에 저장된 key
		currentCaptchaId = res.data.captchaId;
		const url = "data:image/png;base64," + res.data.image;
		captchaImage.src = url;
	} catch(err) {
		console.log("캡챠 이미지 새로고침 실패 : ", err);
	}
}

// 음성듣기
async function playAudio(){
	const audioUrl = "/api/captcha/audio";
	
	if(!currentCaptchaId) {
		alert("먼저 캡챠 이미지를 불러와주세요.");
		return;
	}
	
	try {
		const res = await axiosInstance.get(audioUrl, {
			params: {captchaId: currentCaptchaId},
			responseType: "blob"
		});
		
		const url = URL.createObjectURL(res.data);
		const audio = new Audio(url);
		audio.play();
	} catch(err) {
		console.log("캡챠 음성듣기 실패 : ", err);
	}
}

// 사용자 입력값 검증
async function verifyCaptcha(){
	const answerInput = document.getElementById('captcha-answer');
	const userAnswer = answerInput.value;
	const message = document.getElementsByClassName('captcha-message')[0];
	
	if(!userAnswer){
		alert('보이는 문자를 입력해 주세요.');
		return;
	}
	
	const verifyUrl = "/api/captcha/verify"
	
	try {
		const res = await axiosInstance.post(verifyUrl, new URLSearchParams({
			captchaId: currentCaptchaId,
			answer: userAnswer
		}),
		{headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
	);
		
		if(res.data.success){
			hideCaptchaModal();
		} else {
			message.style.display = 'block';
			answerInput.value = '';
			refreshImage();
		}
	} catch(error) {
		console.error('Error: ', error);
		alert('인증 중 오류가 발생했습니다.');
	}
	
}

window.showCaptchaModal = showCaptchaModal;
