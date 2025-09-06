package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.dto.UserSignup;

@Mapper
public interface UserSignupMapper {
	UserSignup selectUserById(String userId);
	UserSignup selectUserByEmail(String email);
	int insertUser(UserSignup user);
}
