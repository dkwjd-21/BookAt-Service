function onModal() {
  const modal = document.getElementsByClassName("queueModal-overlay")[0];
  if (modal.style.display === "none") {
    modal.style.display = "";
    startCountdown();
  } else {
    modal.style.display = "none";
  }
}

function startCountdown() {
  const modal = document.getElementsByClassName("queueModal-overlay")[0];
  // 대기번호 요소
  const numberElement = document.getElementsByClassName("modal-waitingNum")[0];

  // 현재 대기 순번 저장
  let currentNumber = parseInt(numberElement.textContent);

  if (currentNumber > 0) {
    // 0.5초마다 반복 실행으로 대기 순번 앞당기기
    const interval = setInterval(function () {
      // 번호 앞당기기
      currentNumber -= 5;

      // 앞당긴 번호 화면에 표시하기
      numberElement.textContent = currentNumber;

      // 번호가 0 이하가 되면 멈춘다
      if (currentNumber <= 0) {
        clearInterval(interval);

        // 대기열 모달 화면에서 숨기기
        numberElement.textContent = 0;
        modal.style.display = "none";

        // 1부터 30까지 랜덤 수로 대기번호 초기화 (테스트용)
        const randomNumber = Math.floor(Math.random() * 30);
        numberElement.textContent = randomNumber;
      }
    }, 500);
  }
}
