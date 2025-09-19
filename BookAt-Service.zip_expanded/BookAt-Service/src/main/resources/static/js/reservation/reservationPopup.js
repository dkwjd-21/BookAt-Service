// 공통 변수
let today = new Date();
let eventDay = new Date();
let fromServerEventDate = null;
let queueInterval = null;
let fetchAbortController = null;
let isFetching = false;

// 예약 팝업 초기화
document.addEventListener("DOMContentLoaded", () => {
	
	const steps = document.querySelectorAll(".progress-steps li");
	const sections = document.querySelectorAll(".selection-area");
	const nextBtn = document.getElementById("next-btn");
	const submitBtn = document.getElementById("submit-btn");
	
	// 티켓 타입 유형
	const ticketType = document.getElementById("ticket-type").value;
	
	// 날짜 선택 단계
	let eventDateElement = document.getElementById("eventDate");
	const optionParts = document.querySelectorAll(".option-part");
	const selectedDate = document.getElementById("selected-date");
	const selectedSession = document.getElementById("selected-session");
	
	// 좌석 선택 단계
	
	
	// 인원 선택 단계
	const selects = document.querySelectorAll(".input-count");
	const sumPriceEl = document.getElementById("sumPrice");
	const feeEl = document.getElementById("fee");

	// 주문자 정보 입력 단계
	const userNameInput = document.getElementById("user-name");
	const phoneInput = document.getElementById("phone");
	const emailInput = document.getElementById("email");

	let currentStep = 1;
	let totalPrice = 0;
	
	// 캡챠 모달
	showCaptchaModal();
	
	// 초기 달력 세팅
	initCalendar();
	
	optionParts.forEach((part) => {
		part.addEventListener("click", () => {
			optionParts.forEach((p) => p.classList.remove("selected"));
			part.classList.add("selected");
			if(selectedSession) {
				selectedSession.textContent = part.textContent;
			}
			updateSummary();
		});
	});
	
	if (ticketType === "PERSON_TYPE") {
		selects.forEach((sel) => sel.addEventListener("input", updateSummary));
	}
	
	phoneInput.addEventListener("input", () => {
		const cleaned = phoneInput.value.replace(/[^0-9]/g, "");
		if (phoneInput.value !== cleaned) {
			phoneInput.value = cleaned;
			alert("전화번호는 숫자만 입력해주세요.");
		}
	});
	
	function showStep(step) {
		currentStep = step;

		steps.forEach((s, i) => {
			s.classList.toggle("active", i === currentStep - 1);
		});

		sections.forEach((sec) => {
			sec.classList.toggle("active", sec.getAttribute("data-step") == currentStep);
		});

		nextBtn.style.display = (currentStep === steps.length) ? "none" : "block";
		submitBtn.style.display = (currentStep === steps.length) ? "block" : "none";
	}
	
	// 제목 클릭시 해당 단계로 이동 (현재 단계를 기준으로 그 이전단계만 이동 가능)
	steps.forEach((stepEl, idx) => {
		stepEl.addEventListener("click", () => {
			const targetStep = idx + 1;
			if(targetStep < currentStep) {
				showStep(targetStep);
				
				if (currentStep === 1) {
					if (eventDateElement && eventDateElement.value) {
						const [y, m, d] = eventDateElement.value.split("-").map(Number);
						today = new Date(y, m - 1, d);
						eventDay = new Date(y, m - 1, d);
					}
					
					buildCalendar();
					
					document.querySelectorAll(".option-part").forEach((p) => p.classList.remove("selected"));
					
					if (selectedSession && selectedSession.textContent && selectedSession.textContent !== "선택회차") {
						const chosen = Array.from(document.querySelectorAll(".option-part")).find((p) => p.textContent === selectedSession.textContent);
						if (chosen) {
							chosen.classList.add("selected");
						}
					}
				}
			}
		});
	});
	
	// 다음 단계 버튼
	nextBtn.addEventListener("click", () => {
		
		// step1, 날짜 / 회차 예약 : 회차 선택 필수
		if(currentStep === 1) {
			const chosen = document.querySelector(".option-part.selected");
			const chosenOk = chosen || (selectedSession && selectedSession.textContent && selectedSession.textContent !== '선택회차');
			
			if(!chosenOk) {
				alert("회차를 선택해주세요.");
				return;
			}
		}
		
		// step2, 인원 / 등급 선택 버전
		if (currentStep === 2 && ticketType === "PERSON_TYPE") {
			if(totalPrice <= 0) {
				alert("예약인원을 선택해주세요!");
				return;
			}
		}
		
		// step3, 주문자 정보 입력
		if (currentStep === 3) {
			if (!userNameInput.value.trim()) {
				alert("이름을 입력해주세요.");
				userNameInput.focus();
				return;
			}
			if (!phoneInput.value.trim()) {
				alert("전화번호를 입력해주세요.");
				phoneInput.focus();
				return;
			}
			if (!emailInput.value.trim()) {
				alert("이메일을 입력해주세요.");
				emailInput.focus();
				return;
			}
			if (!emailInput.value.includes("@")) {
				alert("이메일에 @를 포함시켜주세요.");
				emailInput.focus();
				return;
			}
		}

		// step 이동
		if (currentStep < steps.length) {
			showStep(currentStep + 1);
		}
	});
	
	// 결제 후 최종 제출
	submitBtn.addEventListener("click", (e) => {
		e.preventDefault();
		alert("예매 완료되었습니다.");
	});
	
	function updateSummary() {
		const summaryAside = document.querySelector("aside.summary-content");
		
		// 날짜
		if (eventDateElement && eventDateElement.value) {
			const [y, m, d] = eventDateElement.value.split("-").map(Number);
			summaryAside.querySelector("p:nth-of-type(1)").textContent = `${y}년 ${m}월 ${d}일`;
		}
		
		const chosen = document.querySelector(".option-part.selected");
		selectedSession.textContent = chosen ? chosen.textContent : "선택회차";
		
		// 등급/인원
		if (!sumPriceEl || !feeEl) return;
		
		let sum = 0;
		let personSummary = [];
		
		selects.forEach((sel) => {
			const unitPrice = parseInt(sel.getAttribute("data-unit-price"), 10);
			const count = parseInt(sel.value, 10);
			if (count > 0) {
				sum += unitPrice * count;
				
				let label = "";
				if (sel.name === "adultCount") label = "성인";
				else if (sel.name === "youthCount") label = "청소년";
				else if (sel.name === "childCount") label = "유아";
				
				personSummary.push(`<p>${label} ${count}명</p>`);
			}
		});
		
		const fee = Math.floor(sum * 0.1);
		totalPrice = sum + fee;

		sumPriceEl.textContent = sum.toLocaleString() + "원";
		feeEl.textContent = fee.toLocaleString() + " 원";
		
		document.getElementById("summaryTotal").textContent = totalPrice.toLocaleString() + "원";
		document.getElementById("total-price").value = totalPrice;

		const personInfoEl = document.querySelector(".person-info");
		personInfoEl.innerHTML = personSummary.length ? personSummary.join("") : "<p>선택된 인원이 없습니다.</p>";	
	}
	
	function initCalendar() {
		if(eventDateElement && eventDateElement.value) {
			console.log("서버에서 받은 이벤트 날짜:", eventDateElement.value);
			
			const [y, m, d] = eventDateElement.value.split("-").map(Number);
			today = new Date(y, m - 1, d);
			eventDay = new Date(y, m - 1, d);
			if (selectedDate) {
				selectedDate.textContent = `${y}년 ${m}월 ${d}일`;
			}
		} else {
			console.log("지정된 이벤트 날짜가 없어, 오늘 날짜를 기준으로 달력을 생성합니다.");
		}
		buildCalendar();
		updateSummary();
	}
	
});

// 달력 함수 (step 1)
// build Calendar 
function buildCalendar() {
	// tr 행
	let row = null;
	// td 셀
	let cell = null;
	// cnt : 달력에 현재까지 추가된 날짜 셀의 개수
	let cnt = 0;
	// tbody 태그
	let cTBody = document.getElementById("calenderTbody");
	// 제목(이벤트 날짜)
	let cTitle = document.getElementById("Date").children[0];
	// 제목 innerHTML
	cTitle.innerHTML = today.getFullYear() + "년 " + (today.getMonth() + 1) + "월";


	let firstDate = new Date(today.getFullYear(), today.getMonth(), 1);
	let lastDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
	// 작성할 테이블을 초기화
	// 이부분 하지 않으면 prev, next 했을 때 삭제되지 않고 셀이 계속 생김
	while (cTBody.rows.length > 0) {    // 고정행을 tbody에 넣었음. 행이 남아있으면 삭제.
		cTBody.deleteRow(cTBody.rows.length - 1);            // 가장 마지막 행을 삭제함
	}
	// 달의 첫 날까지 빈 셀을 생성
	row = cTBody.insertRow();
	for (i = 0; i < firstDate.getDay(); i++) {
		cell = row.insertCell();
		cnt++;
	}
	// 본격적으로 달력에 요일 채우기
	for (i = 1; i <= lastDate.getDate(); i++) {
		cell = row.insertCell();
		cnt++;
		cell.setAttribute('id', i);         // 셀에 ID 부여 (날짜 1, 2, 3...)
		cell.innerHTML = i;                 // 날짜 숫자 출력 
		cell.align = "center";              // 가운데 정렬

		// 현재 렌더링하는 날짜가 이벤트 날짜와 '년, 월, 일' 모두 일치하는지 확인
		if (i === eventDay.getDate() && today.getMonth() === eventDay.getMonth() && today.getFullYear() === eventDay.getFullYear()) {
			const optionDay = document.querySelector(".option-day");
			optionDay.style.backgroundColor = "#FCECA4";
			optionDay.style.fontWeight = "700";
			
			cell.classList.add('event-day'); // 일치하면 'event-day' 클래스 추가
		} else {
			cell.classList.add('disabled-day');
		}

		if (cnt % 7 == 0) {
			// 전체 셀 개수 7개마다 줄 바꿈                    
			row = cTBody.insertRow();
		}
	}
	// 달력의 마지막 날 뒤 빈칸 행으로 채우기
	if (cnt % 7 != 0) {
		for (i = 0; i < 7 - (cnt % 7); i++) {
			cell = row.insertCell();
		}
	}
}

// prev Calendar (이전 달)
function prevCalendar() {
	today = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
	buildCalendar();
}

// next Calendar (다음 달)
function nextCalendar() {
	today = new Date(today.getFullYear(), today.getMonth() + 1, today.getDate());
	buildCalendar();
}

// default Month (현재 날짜로 이동)
function defaultCalendar() {
	today = eventDay;
	buildCalendar();
}
