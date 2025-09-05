package com.bookat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindPassword {

	@NotBlank(message = "아이디를 입력해주세요.")
	private String userId;
	@NotBlank(message = "전화번호를 입력해주세요.")
	private String userPhone;
	
}
