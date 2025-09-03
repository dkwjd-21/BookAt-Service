package com.bookat.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.UserLogin;

@Mapper
public interface UserMapper {
	
	UserLogin findUserById(String userId);

}
