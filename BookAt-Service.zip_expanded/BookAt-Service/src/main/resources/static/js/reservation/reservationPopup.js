// 공통 변수
let today = new Date();
let eventDay = new Date();
let queueInterval = null;
let fetchAbortController = null;
let isFetching = false;

// 예약 팝업 초기화
document.addEventListener("DOMContentLoaded", () => {
	
	const steps = document.querySelectorAll(".progress-steps li");
	const sections = document.querySelectorAll(".selection-area");
	const nextBtn = document.querySelector("#next-btn");
	const submitBtn = document.querySelector("#submit-btn");
	
	// 티켓 타입 유형
	const ticketType = document.getElementById("ticket-type").value;
	
	// 인원 선택 요소
	const selects = document.querySelectorAll(".input-count");
	const sumPriceEl = document.getElementById("sumPrice");
	const feeEl = document.getElementById("fee");

	// 주문자 정보 입력
	const userNameInput = document.getElementById("user-name");
	const phoneInput = document.getElementById("phone");
	const emailInput = document.getElementById("email");

	let currentStep = 1;
	let totalPrice = 0;
	
	showCaptchaModal();
	
	buildCalendar();
	
	nextBtn.addEventListener("click", () => {
		
		// step1, 날짜 / 회차 예약
		if(currentStep === 1) {
			
			// 날짜 선택 안하면 못넘어가는 조건 하나 추가하기

			let eventDateElement = document.getElementById("eventDate");

			if (eventDateElement && eventDateElement.value) {
				console.log("서버에서 받은 이벤트 날짜:", eventDateElement.value);

				// 'YYYY-MM-DD' 형식의 날짜 문자열을 Date 객체로 변환
				const dateParts = eventDateElement.value.split('-'); // ["YYYY", "MM", "DD"]
				const year = parseInt(dateParts[0], 10);
				const month = parseInt(dateParts[1], 10) - 1; // JavaScript에서 월(month)은 0부터 시작
				const day = parseInt(dateParts[2], 10);

				// today 변수를 서버에서 받은 날짜로 덮어씀
				today = new Date(year, month, day);
				eventDay = new Date(year, month, day);

			} else {
				console.log("지정된 이벤트 날짜가 없어, 오늘 날짜를 기준으로 달력을 생성합니다.");
			}

			buildCalendar();
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

		// Step 이동
		if (currentStep < steps.length) {
		  currentStep++;
		  steps.forEach((s) => s.classList.remove("active"));
		  steps[currentStep - 1].classList.add("active");

		  sections.forEach((sec) => {
		    sec.style.display =
		      sec.getAttribute("data-step") == currentStep ? "block" : "none";
		  });

		  if (currentStep === steps.length) {
		    nextBtn.style.display = "none";
		    submitBtn.style.display = "block";
		  }
		}
		
		// aside 요약 정보 업데이트
		const selectedDayDisplay = document.querySelector(".option-day");
		const summaryAside = document.querySelector("aside.summary-content");
		const summarySession = summaryAside.querySelector("p:nth-of-type(2)");
		let currentSelectedPartOption = null;
		
		selectedDayDisplay.classList.add("selected");
		
		const partContainer = document.querySelector(".select-container");
		if (partContainer) {
		  partContainer.addEventListener("click", (event) => {
		    const clickedPart = event.target.closest(".option-part");
		    if (!clickedPart) return;

		    if (currentSelectedPartOption) {
		      currentSelectedPartOption.classList.remove("selected");
		    }

		    clickedPart.classList.add("selected");
		    currentSelectedPartOption = clickedPart;

		    summarySession.textContent = `${clickedPart.textContent}`;
		  });
		}
		
		// step2, 인원 선택 요약
		if (ticketType === "PERSON_TYPE") {
		  let selectedList = null;
		  if (sumPriceEl) {
		    selectedList = document
		      .querySelector("#sumPrice")
		      .closest("tr")
		      .querySelector("td:last-child");
		  }

		  function updateSummary() {
		    if (!sumPriceEl || !feeEl || !selectedList) return;

		    let sum = 0;
		    let selected = [];
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

		        selected.push(`${label} ${count}명`);
		        personSummary.push(`<p>${label} ${count}명</p>`);
		      }
		    });

		    const fee = Math.floor(sum * 0.1);
		    totalPrice = sum + fee;

		    sumPriceEl.textContent = sum.toLocaleString() + "원";
		    feeEl.textContent = fee.toLocaleString() + " 원";

		    selectedList.innerHTML = selected.length
		      ? selected.map((s) => `<p>${s}</p>`).join("")
		      : "-";

		    document.getElementById("summaryTotal").textContent =
		      totalPrice.toLocaleString() + "원";
		    document.getElementById("total-price").value = totalPrice;

		    const personInfoEl = document.querySelector(".person-info");
		    personInfoEl.innerHTML = personSummary.length
		      ? personSummary.join("")
		      : "<p>선택된 인원이 없습니다.</p>";
		  }

		  selects.forEach((sel) => sel.addEventListener("input", updateSummary));
		}
		
		// step3, 전화번호 숫자만
		phoneInput.addEventListener("input", () => {
		  const cleaned = phoneInput.value.replace(/[^0-9]/g, "");
		  if (phoneInput.value !== cleaned) {
		    phoneInput.value = cleaned;
		    phoneInput.focus();
		    alert("전화번호는 숫자만 입력해주세요.");
		    return;
		  }
		});
		
	});
	
	// 최종 제출
	submitBtn.addEventListener("click", (e) => {
	  e.preventDefault();
	  alert("결제를 진행하겠습니다.");
	});
	
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
