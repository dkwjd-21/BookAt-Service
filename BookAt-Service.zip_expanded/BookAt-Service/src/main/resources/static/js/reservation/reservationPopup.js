// 통합 예약 팝업 스크립트 (reservation-token 기반, 좌석/인원 지원)
// 주석을 꼼꼼히 달아 가독성과 유지보수성을 높였습니다.

// --------------------------------------------------
// 전역(공통) 변수
// --------------------------------------------------
// today, eventDay : 달력 렌더링을 위한 현재/이벤트 기준 날짜
let today = new Date();
let eventDay = new Date();

// 현재 선택된 스케줄(회차) ID
let currentScheduleId = null;

// 선택된 좌석 리스트 (SEAT_TYPE 모드에서 사용)
let selectedSeats = [];

// 총 결제 금액 (인원/좌석 선택 시 업데이트)
let totalPrice = 0;

// --------------------------------------------------
// DOMContentLoaded: 페이지 로드 후 초기화
// --------------------------------------------------
document.addEventListener("DOMContentLoaded", () => {
	// ====== DOM 요소 조회 ======
	const steps = document.querySelectorAll(".progress-steps li");
	const sections = document.querySelectorAll(".selection-area");
	const nextBtn = document.getElementById("next-btn");
	const submitBtn = document.getElementById("submit-btn");

	// 티켓 타입 (SEAT_TYPE 또는 PERSON_TYPE). 엘리먼트가 없으면 안전하게 PERSON_TYPE로 기본값 세팅
	const ticketTypeEl = document.getElementById("ticket-type");
	const ticketType = ticketTypeEl ? ticketTypeEl.value : "PERSON_TYPE";

	// 날짜/회차 관련 DOM
	const eventDateElement = document.getElementById("eventDate");
	const optionParts = document.querySelectorAll(".option-part");
	const selectedDate = document.getElementById("selected-date");
	const selectedSession = document.getElementById("selected-session");

	// 인원 선택(가격 계산) 관련 DOM
	const selects = document.querySelectorAll(".input-count");
	const sumPriceEl = document.getElementById("sumPrice");
	const feeEl = document.getElementById("fee");

	// 주문자 정보 입력란
	const userNameInput = document.getElementById("user-name");
	const phoneInput = document.getElementById("phone");
	const emailInput = document.getElementById("email");

	// 이벤트 ID (DOM에 있을 때만 사용)
	const eventId = document.getElementById("eventId") ? document.getElementById("eventId").value : null;

	// 서버에서 내려준 예약 토큰을 세션스토리지에 저장 (팝업/탭마다 고유한 세션을 갖기 위해)
	const reservationTokenEl = document.getElementById("reservation-token");
	const reservationToken = reservationTokenEl ? reservationTokenEl.value : null;
	if (reservationToken) sessionStorage.setItem("reservationToken", reservationToken);

	// 내부 상태
	window.currentStep = 1;

	// ====== 초기화 작업 ======
	// 캘린더 초기 렌더링 및 요약 갱신
	initCalendar();
	updateSummary();

	// 캡챠가 필요하면 호출 (사용하지 않으면 주석 처리)
	//showCaptchaModal();

	// ====== 회차 클릭 이벤트 처리 ======
	// 회차를 선택하면 회차 UI highlight, selectedSession 갱신, (좌석 타입이면) 좌석 데이터 로드
	optionParts.forEach((part) => {
		part.addEventListener("click", async () => {
			// 다른 회차 선택 제거
			optionParts.forEach((p) => p.classList.remove("selected"));
			part.classList.add("selected");

			// 회차 텍스트를 요약에 반영
			if (selectedSession) selectedSession.textContent = part.textContent;

			// 회차 ID 취득
			const scheduleId = part.getAttribute("data-session-id");
			
			/*
			
			// 회차 수정시 다음단계 값 초기화
			if(currentScheduleId && currentScheduleId !== scheduleId) {
				resetPersonSelection();
				resetSeatSelection();
				totalPrice = 0;
				selectedSeats = [];
			}
			
			currentScheduleId = scheduleId;
			*/

			// 좌석 타입이면 서버에서 좌석 정보를 조회해서 렌더
			if (ticketType === "SEAT_TYPE") {
				const token = sessionStorage.getItem("reservationToken");
				try {
					await fetchSeatInfo(eventId, scheduleId);
				} catch (err) {
					console.error("좌석 정보 로딩 오류:", err);
					alert("좌석 정보를 불러오는 중 오류가 발생했습니다.");
				}
			}

			// 요약 갱신
			updateSummary();
		});
	});

	// ====== 인원 변경 이벤트 (PERSON_TYPE) ======
	if (ticketType === "PERSON_TYPE") {
		selects.forEach((sel) => sel.addEventListener("input", updateSummary));
	}

	// ====== 전화번호 입력 필터링 ======
	// 숫자 외 입력을 제거하고 알림을 띄움 (나중에 토스트로 변경 권장)
	if (phoneInput) {
		phoneInput.addEventListener("input", () => {
			const cleaned = phoneInput.value.replace(/[^0-9]/g, "");
			if (phoneInput.value !== cleaned) {
				phoneInput.value = cleaned;
				alert("전화번호는 숫자만 입력해주세요.");
			}
		});
	}

	// ====== 단계 표시 함수 ======
	function showStep(step) {
		window.currentStep = step;
		steps.forEach((s, i) => s.classList.toggle("active", i === step - 1));
		sections.forEach((sec) => sec.classList.toggle("active", sec.getAttribute("data-step") == step));

		// 마지막 단계이면 다음 버튼 숨기고 제출 버튼 보이기
		nextBtn.style.display = step === steps.length ? "none" : "block";
		submitBtn.style.display = step === steps.length ? "block" : "none";

	}

	// ====== 제목 클릭 시 이전 단계로 이동 ======
	steps.forEach((stepEl, idx) => {
		stepEl.addEventListener("click", async () => {
			const targetStep = idx + 1;

			// 이전 단계만 이동 가능
			if (targetStep < window.currentStep) {
				
				if(window.currentStep === 4 && (targetStep <= 3 || targetStep >= 1)) {
					const token = sessionStorage.getItem("reservationToken");
					if (token) {
						try {
							await axiosInstance.post(`/reservation/${token}/cancel`, {
								reason: "from stage 4 to the previous step",
								isPaymentStep: true
							});
							console.log("step4 → 이전단계 이동: 결제세션 삭제 완료");
						} catch (err) {
							console.error("step4 → 이전단계 이동: 결제세션 삭제 실패", err);
						}
					}
				}
				
				showStep(targetStep);
				
				// 이전단계로 돌아가면 모든값이 초기화되어버려서 일단 조건
				// 회차를 변경했을 때만 초기화되도록하고싶음
				if(ticketType !== "PERSON_TYPE") {
					const token = sessionStorage.getItem("reservationToken");

					// STEP별 초기화 및 상태 복원
					if (targetStep === 1) {
						// STEP1: 달력 초기화, 회차 선택 초기화
						initCalendar();
						resetPersonSelection();

						// STEP2에서 선택된 좌석 서버 초기화 
						if (currentScheduleId && selectedSeats.length > 0) {
							await resetReservationOnServer(token, eventId, currentScheduleId, selectedSeats);
						}
						resetSeatSelection();

						document.querySelectorAll(".option-part").forEach(p => p.classList.remove("selected"));
						if (selectedSession) selectedSession.textContent = "선택회차";
					} else if (targetStep === 2) {
						// STEP2: 좌석/인원 선택 초기화
						resetPersonSelection();
						// STEP2에서 선택된 좌석 서버 초기화 
						if (currentScheduleId && selectedSeats.length > 0) {
							await resetReservationOnServer(token, eventId, currentScheduleId, selectedSeats);
						}
						resetSeatSelection();
					}
				}
				
				// 요약도 갱신
				if (typeof updateSummary === "function") updateSummary();

			}
		});
	});
	
	async function handleReservationError(err, token) {
	  if (!err?.response) {
	    alert("네트워크 오류 발생");
	    return true;
	  }

	  const status = err?.response?.status;
	  const errorMsg = err.response.data?.error || "알 수 없는 오류 발생";

	  // 예약 세션 만료 오류
	  if (status === 410) {
	    alert(
	      errorMsg || "예약 세션이 만료되었습니다. 다시 예약을 진행해주세요."
	    );
	    sessionStorage.removeItem("reservationToken");
	    sessionStorage.removeItem("eventId");
	    try {
	      window.close();
	    } catch (err) {
	      console.log("팝업창 닫기 실패");
	    }
	    return true;
	  }

	  // 좌석 초과 오류
	  if (status === 409) {
	    alert(
	      errorMsg || "잔여좌석을 초과하여 선택할 수 없습니다. 다시 선택해주세요."
	    );
	    return true;
	  }

	  // 기타 오류
	  if (errorMsg) {
	    alert(errorMsg);
	    return true;
	  }

	  console.error("요청 처리 중 에러 발생: ", err);
	  return true;
	}

	// ====== 단계 이동(다음 버튼) 처리 ======
	nextBtn.addEventListener("click", async () => {
		const token = sessionStorage.getItem("reservationToken");
		if (!token) {
			alert("예약 세션이 없습니다. 다시 예약을 시작해주세요.");
			return;
		}
		
		try {
		  await axiosInstance.get(`/reservation/${token}/check`);
		} catch (err) {
		  const handled = await handleReservationError(err, token);
		  if (handled) return;
		}

		// STEP1: 회차 선택 검증 및 서버 등록
		if (window.currentStep === 1) {
			const chosen = document.querySelector(".option-part.selected");
			if (!chosen) {
				alert("회차를 선택해주세요.");
				return;
			}

			const scheduleId = chosen.getAttribute("data-session-id");

			try {
				// 서버에 선택 회차 전달 (API 규칙: /reservation/{token}/step1)
				const res = await axiosInstance.post(`/reservation/${token}/step1`, { scheduleId: parseInt(scheduleId) });
				if (res.data.status === "STEP2") {
					/*
					// 사용자가 회차를 변경했으면 인원/좌석 선택 초기화
					if (currentScheduleId !== scheduleId) {
						resetPersonSelection();
						selectedSeats = [];
						totalPrice = 0;
						currentScheduleId = scheduleId;
					}
					*/
					
					if(currentScheduleId !== scheduleId) {
						// 인원형은 인원/금액 초기화
						resetPersonSelection();
						totalPrice = 0;
						
						// 좌석형은 서버 홀드 좌석 해제 + 프론트 초기화
						if (ticketType === "SEAT_TYPE" && currentScheduleId && selectedSeats.length > 0) {
							await resetReservationOnServer(token, eventId, currentScheduleId, selectedSeats);
						}
						
						resetSeatSelection();
						
						currentScheduleId = scheduleId;
					}
					
					showStep(2);
				} else {
					console.warn("서버에서 STEP2로 넘어가지 않음", res.data);
				}
			} catch (err) {
				console.error("회차 선택 오류:", err);
				// handleReservationError 같은 공통 에러 핸들러를 만들어 재사용 가능
				await handleReservationError(err, token);
			}
			return;
		}

		// STEP2: 인원 선택 (PERSON_TYPE) 또는 좌석 선택 (SEAT_TYPE)
		if (window.currentStep === 2) {
			if (ticketType === "PERSON_TYPE") {
				if (totalPrice <= 0) {
					alert("예약인원을 선택해주세요!");
					return;
				}

				// payload 예시: 서버는 personCounts와 totalPrice를 기대
				const payload = {
					ticketType: "PERSON_TYPE",
					personCounts: {
						ADULT: parseInt(document.getElementById("adultCount").value) || 0,
						YOUTH: parseInt(document.getElementById("youthCount").value) || 0,
						CHILD: parseInt(document.getElementById("childCount").value) || 0,
					},
					totalPrice: parseInt(document.getElementById("total-price").value) || totalPrice,
				};

				try {
					const res = await axiosInstance.post(`/reservation/${token}/step2`, payload);
					if (res.data.status === "STEP3") showStep(3);
					else console.warn("서버 응답으로 STEP3가 아님", res.data);
				} catch (err) {
					console.error("인원 선택 오류:", err);
					await handleReservationError(err, token);
				}

			} else if (ticketType === "SEAT_TYPE") {
				// 좌석 선택 모드
				if (!selectedSeats || selectedSeats.length === 0) {
					alert("좌석을 선택해 주세요!");
					return;
				}

				const payload = {
					ticketType: "SEAT_TYPE",
					eventId: parseInt(eventId),
					scheduleId: parseInt(currentScheduleId),
					seatNames: selectedSeats,
					totalPrice: totalPrice
				};

				try {
					const res = await axiosInstance.post(`/reservation/${token}/step2`, payload);

					if (res.data.status === "STEP3") {
						showStep(3);
					} else {
						// 서버에서 STEP3가 아니면, 검증 실패나 예외 상황
						alert(res.data?.message || "좌석 검증에 실패했습니다.");
						if (currentScheduleId && eventId) {
							await fetchSeatInfo(eventId, currentScheduleId); // 좌석 다시 그림
						}
					}
				} catch (err) {
					if (err.response && err.response.status === 409) {
						// 좌석 충돌(이미 예약됨)
						alert(err.response.data?.message || "이미 예약된 좌석이 포함되어 있습니다.");
					} else {
						alert("좌석 검증 중 오류가 발생했습니다.");
					}
					console.error("좌석 선택 오류:", err);
					// 최신 좌석 다시 불러오기
					if (currentScheduleId && eventId) await fetchSeatInfo(eventId, currentScheduleId);
				}
			}
			return;
		}

		// STEP3: 주문자 정보
		if (window.currentStep === 3) {
			if (!userNameInput?.value?.trim()) {
				alert("이름을 입력해주세요.");
				userNameInput?.focus();
				return;
			}
			if (!phoneInput?.value?.trim()) {
				alert("전화번호를 입력해주세요.");
				phoneInput?.focus();
				return;
			}
			if (!emailInput?.value?.trim() || !emailInput.value.includes("@")) {
				alert("올바른 이메일을 입력해주세요.");
				emailInput?.focus();
				return;
			}

			const payload = {
				userName: userNameInput.value.trim(),
				phone: phoneInput.value.trim(),
				email: emailInput.value.trim(),
			};

			try {
				const token = sessionStorage.getItem("reservationToken");
				const res = await axiosInstance.post(`/reservation/${token}/step3`, payload);
				if (res.data.status === "STEP4") {
					
					// 결제 프래그먼트 렌더링
					const url = res.data.paymentStepUrl;
					const html = await axiosInstance.get(url, { params: {token} });
					document.querySelector('#event-payment-frag').innerHTML = html.data;
					
					// 프래그먼트 안의 결제 버튼 클릭시 실행되는 결제 진행 함수
					requestAnimationFrame(() => {
						bindPayFragment()
						.then(() => {
							console.log("[PAY] 프래그먼트 바인딩 성공");
						})
						.catch((err) => {
							console.error("[PAY] 프래그먼트 바인딩 실패", err);
						});

					})
					
					showStep(4);
				}
				else {
					console.warn("주문자 정보 저장 실패 응답:", res.data);
				}
			} catch (err) {
				console.error("사용자 정보 저장 오류:", err);
				await handleReservationError(err, token);
			}
			return;
		}

		// 기본: 다음 단계로 이동
		if (window.currentStep < steps.length) showStep(window.currentStep + 1);
	});

	// ====== 제출(결제) 버튼 ======
	submitBtn.addEventListener("click", async (e) => {
		e.preventDefault();
		const token = sessionStorage.getItem("reservationToken");
		if (!token) {
			alert("예약 세션이 없습니다. 다시 예약을 시작해주세요.");
			return;
		}

		try {
			// 결제 직전 서버 확인(토큰 유효성 등)
			await axiosInstance.get(`/reservation/${token}/check`);

			// 서버에 예매 확정 요청 (좌석 유형인 경우만)
			if (ticketType === "SEAT_TYPE") {
				const payload = {
					eventId: parseInt(eventId),
					scheduleId: parseInt(currentScheduleId),
					seatNames: selectedSeats,
					totalPrice: totalPrice
				};
				
				await axiosInstance.post(`/reservation/${token}/confirmBooking`, payload);
			}
			// 완료버튼 클릭 시 레디스 세션 정보 삭제
			const res = await axiosInstance.post('/reservation/complete', { token });
			
			if(res.data?.status === 'SUCCESS') {
				alert(res.data?.message);
				//alert("예매가 완료되었습니다");
				sessionStorage.removeItem("reservationToken");
				sessionStorage.removeItem("eventId");
				try { window.close(); } catch (err) { console.warn("팝업 닫기 실패:", err); }
			} else {
				console.error("예매 완료 처리 오류:", err);
				await handleReservationError(err, token);
			}
		} catch (err) {
			console.error("결제 확인 오류:", err);
			await handleReservationError(err, token);
			return;
		}
	});

	// ====== 요약(aside) 업데이트 함수 ======
	function updateSummary() {
		const summaryAside = document.querySelector("aside.summary-content");
		if (!summaryAside) return;

		// 날짜 요약
		if (eventDateElement && eventDateElement.value) {
			const [y, m, d] = eventDateElement.value.split("-").map(Number);
			summaryAside.querySelector("p:nth-of-type(1)").textContent = `${y}년 ${m}월 ${d}일`;
		}

		// 선택된 회차 텍스트
		const chosen = document.querySelector(".option-part.selected");
		if (selectedSession) selectedSession.textContent = chosen ? chosen.textContent : "선택회차";

		// PERSON_TYPE: 인원별 합계 계산
		if (ticketType === "PERSON_TYPE") {
			if (!sumPriceEl || !feeEl) return;
			let sum = 0;
			let personSummary = [];

			selects.forEach((sel) => {
				const unitPrice = parseInt(sel.getAttribute("data-unit-price"), 10) || 0;
				const count = parseInt(sel.value, 10) || 0;
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
			feeEl.textContent = fee.toLocaleString() + "원";
			const summaryTotalEl = document.getElementById("summaryTotal");
			if (summaryTotalEl) summaryTotalEl.textContent = totalPrice.toLocaleString() + "원";
			const personInfoEl = document.querySelector(".person-info");
			if (personInfoEl) personInfoEl.innerHTML = personSummary.length ? personSummary.join("") : "<p>선택된 인원이 없습니다.</p>";

			// SEAT_TYPE: 선택된 좌석 요약
		} else if (ticketType === "SEAT_TYPE") {
			const selectedSeatsInfo = document.getElementById('selected-seats-info');
			const seatCountInfo = document.getElementById('seat-count-info');

			const seatNames = selectedSeats.slice(); // 복사본
			const seatCount = seatNames.length;

			if (selectedSeatsInfo) selectedSeatsInfo.textContent = seatCount > 0 ? `좌석: ${seatNames.join(', ')}` : '선택된 좌석이 없습니다.';
			if (seatCountInfo) seatCountInfo.textContent = seatCount > 0 ? `총 ${seatCount}매` : '';

			// 총 금액은 totalPrice 변수를 사용 (선택 시 업데이트됨)
			const summaryTotalEl = document.getElementById("summaryTotal");
			if (summaryTotalEl) summaryTotalEl.textContent = totalPrice.toLocaleString() + "원";
		}
	}

	// 전역 노출
	window.updateSummary = updateSummary;

	// ====== 인원/요약 초기화 ======
	function resetPersonSelection() {
		selects.forEach((sel) => { sel.value = 0; });
		totalPrice = 0;
		if (sumPriceEl) sumPriceEl.textContent = "0원";
		if (feeEl) feeEl.textContent = "0원";
		const summaryTotalEl = document.getElementById("summaryTotal");
		if (summaryTotalEl) summaryTotalEl.textContent = "0원";
		const personInfoEl = document.querySelector(".person-info");
		if (personInfoEl) personInfoEl.innerHTML = "<p>선택된 인원이 없습니다.</p>";
	}

	// ====== 캘린더 초기화 함수 ======
	function initCalendar() {
		if (eventDateElement && eventDateElement.value) {
			const [y, m, d] = eventDateElement.value.split("-").map(Number);
			today = new Date(y, m - 1, d);
			eventDay = new Date(y, m - 1, d);
			if (selectedDate) selectedDate.textContent = `${y}년 ${m}월 ${d}일`;
		}
		buildCalendar();
	}

	// 초기 요약 한 번 더 갱신
	updateSummary();
});





// --------------------------------------------------
// 달력 렌더링 관련 함수 (기존 구현 유지)
// --------------------------------------------------
function buildCalendar() {
	const cTBody = document.getElementById("calenderTbody");
	const cTitle = document.getElementById("Date")?.children?.[0];
	if (!cTBody || !cTitle) return;

	cTitle.innerHTML = today.getFullYear() + "년 " + (today.getMonth() + 1) + "월";

	const firstDate = new Date(today.getFullYear(), today.getMonth(), 1);
	const lastDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);

	// tbody 초기화
	while (cTBody.rows.length > 0) cTBody.deleteRow(cTBody.rows.length - 1);

	let row = cTBody.insertRow();
	let cnt = 0;

	// 빈 칸 채우기
	for (let i = 0; i < firstDate.getDay(); i++) { row.insertCell(); cnt++; }

	// 날짜 채우기
	for (let i = 1; i <= lastDate.getDate(); i++) {
		const cell = row.insertCell();
		cnt++;
		cell.setAttribute('id', i);
		cell.innerHTML = i;
		cell.align = 'center';

		if (i === eventDay.getDate() && today.getMonth() === eventDay.getMonth() && today.getFullYear() === eventDay.getFullYear()) {
			const optionDay = document.querySelector('.option-day');
			if (optionDay) {
				optionDay.style.backgroundColor = '#FCECA4';
				optionDay.style.fontWeight = '700';
			}
			cell.classList.add('event-day');
		} else {
			cell.classList.add('disabled-day');
		}

		if (cnt % 7 === 0) row = cTBody.insertRow();
	}

	// 마지막 행 빈칸 채우기
	if (cnt % 7 !== 0) {
		for (let i = 0; i < 7 - (cnt % 7); i++) row.insertCell();
	}
}

function prevCalendar() { today = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate()); buildCalendar(); }
function nextCalendar() { today = new Date(today.getFullYear(), today.getMonth() + 1, today.getDate()); buildCalendar(); }
function defaultCalendar() { today = eventDay; buildCalendar(); }

// --------------------------------------------------
// 예약 취소 동기/비동기 함수 (페이지 종료 시 호출)
// --------------------------------------------------
async function cancelReservationSync() {
	const token = sessionStorage.getItem('reservationToken');
	if (!token) return;
	const isPaymentStep = window.currentStep === 4;
	const url = `/reservation/${token}/cancel`;
	const data = JSON.stringify({ reason: 'popup close', isPaymentStep });
	const blob = new Blob([data], { type: 'application/json' });
	const ok = navigator.sendBeacon(url, blob);
	if (!ok) {
		// fallback
		fetch(url, { method: 'POST', body: data, headers: { 'Content-Type': 'application/json' }, keepalive: true })
			.catch(err => console.warn('cancelReservationSync fallback error:', err));
	}
	
	sessionStorage.removeItem('reservationToken');
	sessionStorage.removeItem('eventId');
}

// 직접 취소
async function cancelReservation() {
	const token = sessionStorage.getItem('reservationToken');
	if (!token) return;
	
	const isPaymentStep = window.currentStep === 4;
	
	try {
		const res = await axiosInstance.post(`/reservation/${token}/cancel`, {
			reason: 'cancel', isPaymentStep
		});

		alert(res.data.message || '예약이 취소되었습니다.');
		sessionStorage.removeItem('reservationToken');
		sessionStorage.removeItem('eventId');
	} catch (err) {
		console.error('cancelReservation error:', err);
		await handleReservationError(err, token);
	}
}

window.addEventListener('beforeunload', cancelReservationSync);
window.addEventListener('pagehide', cancelReservationSync);

// --------------------------------------------------
// 좌석 관련: 서버에서 좌석 데이터 조회 및 렌더링
// --------------------------------------------------
async function fetchSeatInfo(eventId, scheduleId) {
	if (!eventId || !scheduleId) throw new Error("eventId와 scheduleId가 필요합니다.");

	try {
		const res = await axiosInstance.get("/reservation/seat/getSeats", {
			params: { eventId, scheduleId },
			withCredentials: true,
			headers: { "X-Requested-With": "XMLHttpRequest" },
		});

		resetSeatSelection();
		renderSeats(res.data);
	} catch (err) {
		console.error("fetchSeatInfo error:", err);
		throw err;
	}
}


// 좌석 렌더링: 행 단위로 그룹화해서 DOM 생성
function renderSeats(seats) {
	const container = document.querySelector('.stage-seat');
	if (!container) return;
	container.innerHTML = '';

	let currentRow = '';
	let rowDiv = null;

	seats.forEach(seat => {
		const rowName = seat.seatName ? seat.seatName.charAt(0) : '?';

		if (rowName !== currentRow) {
			if (rowDiv) container.appendChild(rowDiv);
			rowDiv = document.createElement('div');
			rowDiv.className = 'seat-row';

			const rowLabel = document.createElement('div');
			rowLabel.className = 'row-label';
			rowLabel.textContent = rowName;
			rowDiv.appendChild(rowLabel);

			currentRow = rowName;
		}

		const seatEl = document.createElement('div');
		seatEl.className = 'seat';
		seatEl.dataset.seatName = seat.seatName || '';

		if (seat.status === 'AVAILABLE') {
			seatEl.classList.add('seat-available');
			// 클릭 시 선택/해제
			seatEl.addEventListener('click', () => toggleSeatSelection(seatEl, seat));
		} else {
			seatEl.classList.add('seat-unavailable');
		}

		rowDiv.appendChild(seatEl);
	});

	if (rowDiv) container.appendChild(rowDiv);
}

// 좌석 선택/해제 로직
function toggleSeatSelection(el, seatObj = null) {
	if (!el) return;
	const seatName = el.dataset.seatName;
	const already = el.classList.contains('seat-selected');

	if (already) {
		// 선택 해제
		el.classList.remove('seat-selected');
		selectedSeats = selectedSeats.filter(s => s !== seatName);
	} else {
		// 선택 제한(예: 2석) 체크
		const MAX_SELECTION = 2; // 필요시 서버/환경 설정으로 변경
		if (selectedSeats.length >= MAX_SELECTION) {
			alert(`최대 ${MAX_SELECTION}개까지 선택 가능합니다.`);
			return;
		}

		el.classList.add('seat-selected');
		selectedSeats.push(seatName);
		console.log(selectedSeats);
	}

	// 총 금액 계산: 좌석 객체에 price가 있으면 사용, 없으면 기본값(예: 10000원)
	const eventPriceEl = document.getElementById("eventPrice");
	const unitPrice = eventPriceEl ? parseInt(eventPriceEl.value, 10) : 10000;
	
	totalPrice = selectedSeats.length * unitPrice;

	// 좌석 선택/해제 후 요약 갱신
	if (typeof updateSummary === 'function') updateSummary();
}

// 좌석 유효성 검증 (서버에게 전달)
// - 이 함수는 필요시 사용. 현재는 step2에서 axios로 검증 요청을 보냄.
async function validateSeatsOnServer(token, eventId, scheduleId, seatNames) {
	try {
		const res = await fetch(`/reservation/${token}/seat/validateSeats`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ eventId, scheduleId, seatNames })
		});

		if (!res.ok) {
			// 실패 시 최신 좌석 상태를 불러오고 false 반환
			const tokenLocal = sessionStorage.getItem('reservationToken');
			if (tokenLocal && eventId && scheduleId) await fetchSeatInfo(eventId, scheduleId);
			return false;
		}

		return true;
	} catch (err) {
		console.error('validateSeatsOnServer error:', err);
		return false;
	}
}

// 좌석 새로고침 버튼(화면에서 호출 가능)
function refreshSeats() {
	const chosen = document.querySelector('.option-part.selected');
	const scheduleId = chosen ? chosen.getAttribute('data-session-id') : null;
	const eventId = document.getElementById('eventId') ? document.getElementById('eventId').value : null;
	resetSeatSelection();
	if (eventId && scheduleId) fetchSeatInfo(eventId, scheduleId).catch(err => console.error(err));
}

// 선택한 좌석 초기화 (프론트에서만 초기화)
function resetSeatSelection() {
	// 선택한 좌석 데이터 초기화
	selectedSeats = [];
	totalPrice = 0;

	// 화면의 모든 좌석에서 selected 클래스 제거 
	const selectedSeatEls = document.querySelectorAll('.seat-row .seat-selected');
	selectedSeatEls.forEach(el => {
		el.classList.remove('seat-selected');
	})

	if (typeof updateSummary === 'function') updateSummary();
}

// 선택한 좌석/예약 상태 초기화 (서버 Redis/DB 반영)
async function resetReservationOnServer(token, eventId, scheduleId, seats = []) {
	if (!token || !eventId || !scheduleId || seats.length === 0) return;

	try {
		await axiosInstance.post(`/reservation/${token}/reset`, {
			eventId: parseInt(eventId),
			scheduleId: parseInt(scheduleId),
			seatNames: seats
		});
		console.log("서버 예약 상태 초기화 완료");
	} catch (err) {
		console.error("서버 예약 초기화 오류: ", err);
	}
}

// 캡챠 모달 열기 함수 (콜백 받아서 저장)
function showCaptchaModal() {
	const modal = document.getElementById("captchaModal");
	modal.style.display = "flex";
	refreshImage();
}
