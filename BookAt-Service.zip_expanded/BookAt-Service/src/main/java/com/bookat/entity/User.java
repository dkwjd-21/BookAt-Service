package com.bookat.entity;

import lombok.Data;

@Data
public class User {

	private String userId;
	private String userPw;
	private String userName;
	private String email;
	private String phone;
	private String birth;
	private String refreshToken;
	
}
