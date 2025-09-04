package com.bookat.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.bookat.entity.User;

@Mapper
public interface UserMapper {
	
	User findUserById(String userId);
	void updateUserRefreshToken(Map<String, String> values);

}
