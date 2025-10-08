async function goReservationDetails() {
	try {
		const res = await axiosInstance.get("/myPage/reservationDetails");
		console.log(res.data.status);
		
		const reservationContainer = document.getElementById("reservation-container");
		reservationContainer.classList.remove("reservation-hidden");
		
	} catch(err) {
		console.log(err);
	}
};
