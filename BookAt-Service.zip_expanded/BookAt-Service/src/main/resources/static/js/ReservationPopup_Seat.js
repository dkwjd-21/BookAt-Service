// 디폴트값은 금일 날짜.
let today = new Date();
// 이벤트 날짜
let eventDay = new Date();

// 페이지 로드 후 실행할 이벤트
window.onload = () => {
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


	// 날짜 & 회차 선택 처리를 위한 변수
	const selectedDayDisplay = document.querySelector('.option-day');

	// aside 요약 정보 영역
	const summaryAside = document.querySelector('aside.summary-content');
	const summarySession = summaryAside.querySelector('p:nth-of-type(2)'); // '선택회차' p 태그

	let currentSelectedPartOption = null; // 현재 선택된 회차 option을 추적하기 위한 변수

	selectedDayDisplay.classList.add('selected');
	
	// 회차 선택 -> css 변경 & aside 정보 변경 
	const partContainer = document.querySelector('.select-container');
	if (partContainer) {
		partContainer.addEventListener('click', (event) => {
			const clickedPart = event.target.closest('.option-part');

			// .option-part 요소가 아닌 곳을 클릭하면 무시한다.
			if (!clickedPart) {
				return;
			}

			// 이전에 선택된 회차가 있다면 'selected' 클래스를 제거한다.
			if (currentSelectedPartOption) {
				currentSelectedPartOption.classList.remove('selected');
			}

			// 새로 클릭된 회차에 'selected' 클래스를 추가한다.
			clickedPart.classList.add('selected');
			currentSelectedPartOption = clickedPart; // 현재 선택된 회차 업데이트

			// aside 요약 정보 업데이트
			summarySession.textContent = `${clickedPart.textContent}`;
		});
	}

}

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