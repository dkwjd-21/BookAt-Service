package com.bookat.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSignup {
	private String userId;
	private String userPw;
	private String userName;
	private String email;
	private String phone;
	private Date birth;
}
