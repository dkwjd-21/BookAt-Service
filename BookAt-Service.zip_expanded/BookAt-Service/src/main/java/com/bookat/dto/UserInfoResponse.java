package com.bookat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserInfoResponse {
	
	private String userId;
	private String userName;
	private String email;
	private String phone;
	private String birth;
	
}
