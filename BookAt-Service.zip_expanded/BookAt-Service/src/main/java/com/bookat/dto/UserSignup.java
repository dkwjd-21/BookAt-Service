package com.bookat.dto;

import java.util.Date;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UserSignup {
	private String userId;
	private String userPw;
	private String userName;
	private String email;
	private String phone;
	private Date birth;
}
