$(document).ready(function() {

    $("#find-pw-section").show();
	$("#change-pw-section, #result-section").hide();

    // 전화번호 숫자만 입력 체크
    $("#phone").on("input", function() {
        const cleaned = this.value.replace(/[^0-9]/g, '');
        if (this.value !== cleaned) {
            this.value = cleaned;
            $("#find-phone-error").show();
        } else {
            $("#find-phone-error").hide();
        }
    });

    // 비밀번호 찾기 제출
    $("#find-pw-section").submit(function(e) {
        e.preventDefault();

		const userId = $("#userId").val().trim();
        const phone = $("#phone").val().trim();
		
		if(userId === "") {
            $("#find-id-error").text("아이디를 입력해주세요.").show();
            $("#find-phone-error").hide();
            $("#userId").focus();
            return;
        } else {
            $("#find-id-error").hide();
        }

		if(phone === "" || !/^[0-9]+$/.test(phone)) {
            $("#find-phone-error").text("전화번호를 제대로 입력해주세요.").show();
            $("#phone").focus();
            return;
        } else {
            $("#find-phone-error").hide();
        }

        $.ajax({
			url: "/user/findPw",
            type: "POST",
            data: { userId: userId, phone: phone },
            success: function(data) {
				$("#find-id-error").hide();
				$("#hidden-user-id").val(data);
				$("#find-pw-section").hide();
				$("#change-pw-section").show();
            },
            error: function(xhr) {
				const errMsg = xhr.responseText;
				
				// 아이디 관련 에러
				if (errMsg.includes("아이디")) {
				    $("#find-id-error").text(errMsg).show();
				    $("#userId").focus().addClass("input-error");
				}
				// 비밀번호 관련 에러
				else if (errMsg.includes("전화번호")) {
				    $("#find-phone-error").text(errMsg).show();
				    $("#phone").focus().addClass("input-error");
				}
				// 그 외 (공통 에러)
				else {
				    alert(errMsg);
				}
            }
        });
    });

    // 비밀번호 변경 제출
    $("#change-pw-section").submit(function(e) {
        e.preventDefault();

        const password = $("#userPw").val().trim();
        const passwordCheck = $("#userPwCheck").val().trim();
        const userId = $("#hidden-user-id").val().trim();

		if(password === "") {
            $("#find-pw-check").text("비밀번호를 입력해주세요.").show();
			$("#userPw").focus();
            return;
        }
		
		if(passwordCheck === "") {
		    $("#find-pw-check").text("비밀번호를 한번 더 입력해주세요.").show();
			$("#userPwCheck").focus();
		    return;
		}
		
        if(password !== passwordCheck) {
            $("#find-pw-check").text("입력하신 비밀번호가 일치하지 않습니다.").show();
            $("#userPwCheck").focus();
            return;
        }
		
        $("#pw-mismatch-error").hide();

        $.ajax({
			url: "/user/changePassword",
            type: "POST",
            data: { userId: userId, password: password },
            success: function(res) {
                if(res.success) {
                    $("#change-pw-section").hide();
                    $("#result-section").show();
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
    $("#go-login").click(function() { location.href="/user/login"; });
    $("#go-main").click(function() { location.href="/"; });

});
