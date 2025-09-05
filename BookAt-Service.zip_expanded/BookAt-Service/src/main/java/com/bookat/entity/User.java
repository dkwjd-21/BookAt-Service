package com.bookat.entity;

import lombok.Data;

@Data
public class User {

	private String userId;
	private String userPw;
	private String userName;
	private String userPhone;
	private String refreshToken;
	
}
