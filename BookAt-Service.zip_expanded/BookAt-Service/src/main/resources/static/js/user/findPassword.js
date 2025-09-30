$(document).ready(function() {

	$("#verify-btn").hide();
    $("#find-pw-section").show();
	$("#change-pw-section, #result-section").hide();

    // 비밀번호 찾기 제출
    $("#find-pw-section").submit(function(e) {
        e.preventDefault();

		const userId = $("#userId").val().trim();
		
		if(userId === "") {
            $("#find-id-error").text("아이디를 입력해주세요.").show();
            $("#userId").focus();
            return;
        } else {
            $("#find-id-error").hide();
        }

        $.ajax({
			url: "/user/findPw",
            type: "POST",
            data: { userId: userId},
            success: function(data) {
				$("#hidden-user-id").val(data);
				$("#id-check-container").hide();
				$("#verify-btn").show();
            },
            error: function(xhr) {
				const errMsg = xhr.responseText;
				
				// 아이디 관련 에러
				if (errMsg.includes("아이디")) {
				    $("#find-id-error").text(errMsg).show();
				    $("#userId").focus().addClass("input-error");
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
		
		if(password.length < 4) {
		    $("#find-pw-check").text("비밀번호는 4자 이상이어야 합니다.").show();
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
