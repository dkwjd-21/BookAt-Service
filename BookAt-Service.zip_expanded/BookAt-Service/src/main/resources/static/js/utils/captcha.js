
document.addEventListener("DOMContentLoaded", function() {
});

// 캡챠 모달 열기 함수 (콜백 받아서 저장)
function showCaptchaModal(callbackAfterSuccess) {
    const modal = document.getElementById('captchaModal');
    modal.style.display = 'block';
    // 인증 성공 시 실행할 콜백 저장
    window.captchaSuccessCallback = callbackAfterSuccess;
}

// 캡챠 모달을 숨기는 함수
function hideCaptchaModal() {
    const modal = document.getElementById('captchaModal');
    modal.style.display='none';
}

// 이미지 새로고침
function refreshImage(){
	const captchaImage = document.getElementById('captchaImg');
	
	captchaImage.src = "/api/captcha/image";
}

// 음성듣기
function playAudio(){
	const audioUrl = "/api/captcha/audio";
	const audio = new Audio(audioUrl);
	audio.play();
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
		const response = await fetch(verifyUrl, {
			method: 'POST',
			headers: {
				'Content-Type' : 'application/x-www-form-urlencoded',
			}, 
			body : 'answer=' + encodeURIComponent(userAnswer)
		});
		
		const result = await response.json(); 
		
		if(result.success){
			hideCaptchaModal();
			
			// 성공 시 콜백 실행
			if (typeof window.captchaSuccessCallback === "function") {
			    window.captchaSuccessCallback();
			    window.captchaSuccessCallback = null;
			}
			
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