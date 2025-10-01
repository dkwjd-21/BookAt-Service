package com.bookat.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoReqDto {

	// step3
	
	private String userName;
	private String phone;
	private String email;
	
}
