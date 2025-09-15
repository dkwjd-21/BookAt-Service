package com.bookat.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

	private String userId;
	private String userPw;
	private String userName;
	private String email;
	private String phone;
	private String birth;
	private String refreshToken;
	
}
