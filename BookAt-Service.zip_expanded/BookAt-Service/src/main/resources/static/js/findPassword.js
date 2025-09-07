$(document).ready(function() {

    $("#findPwSection").show();
	$("#changePwSection, #resultSection").hide();

    // 전화번호 숫자만 입력 체크
    $("#phone").on("input", function() {
        const cleaned = this.value.replace(/[^0-9]/g, '');
        if (this.value !== cleaned) {
            this.value = cleaned;
            $("#findPhoneError").show();
        } else {
            $("#findPhoneError").hide();
        }
    });

    // 비밀번호 찾기 제출
    $("#findPwSection").submit(function(e) {
        e.preventDefault();

		const userId = $("#userId").val().trim();
        const phone = $("#phone").val().trim();
		
		if(userId === "") {
            $("#findIdError").text("아이디를 입력해주세요.").show();
            $("#findPhoneError").hide();
            $("#userId").focus();
            return;
        } else {
            $("#findIdError").hide();
        }

		if(phone === "" || !/^[0-9]+$/.test(phone)) {
            $("#findPhoneError").text("전화번호를 제대로 입력해주세요.").show();
            $("#phone").focus();
            return;
        } else {
            $("#findPhoneError").hide();
        }

        $.ajax({
            type: "POST",
            url: "/api/user/findPw",
            data: { userId: userId, phone: phone },
            success: function(res) {
                if(res.success) {
					$("#findIdError").hide();
					$("#hiddenUserId").val(userId);
					$("#findPwSection").hide();
					$("#changePwSection").show();
                } else {
                    $("#findError").text(res.message).show();
                }
            },
            error: function() {
                alert("서버 오류 발생");
            }
        });
    });

    // 비밀번호 변경 제출
    $("#changePwSection").submit(function(e) {
        e.preventDefault();

        const password = $("#userPw").val().trim();
        const passwordCheck = $("#userPwCheck").val().trim();
        const userId = $("#hiddenUserId").val().trim();

		if(password === "" || passwordCheck === "") {
            $("#findPwError").text("비밀번호를 입력해주세요.").show();
            return;
        }
		
        if(password !== passwordCheck) {
            $("#findPwError").text("입력하신 비밀번호가 일치하지 않습니다.").show();
            $("#userPwCheck").focus();
            return;
        }
		
        $("#pwMismatchError").hide();

        $.ajax({
            type: "POST",
            url: "/api/user/changePassword",
            data: { userId: userId, password: password },
            success: function(res) {
                if(res.success) {
                    $("#changePwSection").hide();
                    $("#resultSection").show();
                } else {
                    alert(res.message);
                }
            },
            error: function() {
                alert("서버 오류 발생");
            }
        });
    });

    // 결과 버튼
    $("#goLogin").click(function() { location.href="/api/user/login"; });
    $("#goMain").click(function() { location.href="/"; });

});
