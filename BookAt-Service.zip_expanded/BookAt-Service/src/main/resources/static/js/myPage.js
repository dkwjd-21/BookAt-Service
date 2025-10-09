async function goReservationDetails() {
	try {
		const res = await axiosInstance.get("/myPage/reservationDetails");
		const { status, reservations } = res.data;
		
		const reservationContainer = document.getElementById("reservation-container");
		const reservationMain = document.getElementById("reservation-main");
		const template = document.getElementById("reservation-template");
		
		reservationContainer.classList.remove("reservation-hidden");
		reservationMain.innerHTML = "";
		
		if(reservations.length === 0) {
			reservationMain.innerHTML = '<p class="no-reservation-info">예매 내역이 없습니다.</p>';
			return;
		}
		
		reservations.forEach(reservation => {
			const clone = template.cloneNode(true);
			
			const img = clone.querySelector(".event-post img");
			img.src = reservation.eventImg;
			
			clone.querySelector(".event-name").textContent = reservation.eventName;
			clone.querySelector(".schedule-info").textContent = formatDate(reservation.scheduleTime) + ` ( ${reservation.scheduleName} )`;
			clone.querySelector(".reserved-count").textContent = `${reservation.reservedCount}명`;
			clone.querySelector(".reservation-status").textContent = formatDate(reservation.reservationDate) + ` ( ${reservation.reservationStatus} )`;
			clone.querySelector(".total-price").textContent = `${reservation.totalPrice}원`;
			
			reservationMain.appendChild(clone);
		})
		
	} catch(err) {
		alert(err.response?.data.error || "예약정보를 불러오지 못했습니다.");
		return;
	}
}

// 날짜 포맷 변경
function formatDate(dateInfo) {
	const date = new Date(dateInfo);
	const year = date.getFullYear();
	const month = date.getMonth() + 1;
	const day = date.getDate();
	const hour = date.getHours();
	const minute = date.getMinutes();
	
	return `${year}년 ${month}월 ${day}일 ${hour}시 ${minute}분`;
}
