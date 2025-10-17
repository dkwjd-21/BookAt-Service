package com.bookat.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookat.dto.UserSignup;
import com.bookat.mapper.UserSignupMapper;
import com.bookat.service.UserSignupService;

@Service
public class UserSignupServiceImpl implements UserSignupService {

	// 매퍼
	@Autowired
	private UserSignupMapper mapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserSignup getUserById(String userId) {
		return mapper.selectUserById(userId);
	}

	@Override
	public UserSignup getUserByEmail(String email) {
		return mapper.selectUserByEmail(email);
	}

	@Override
	public int insertUser(UserSignup user) {
		// 전화번호 010XXXXOOOO -> 010-XXXX-0000으로 포맷 변경
		String phone = user.getPhone();

		String formattedPhone = phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);

		System.out.println("전화번호 포맷 변경 : " + phone + " -> " + formattedPhone);

		user.setPhone(formattedPhone);

		// 비밀번호 암호화
		String encodedPassword = passwordEncoder.encode(user.getUserPw());
		user.setUserPw(encodedPassword);

		return mapper.insertUser(user);
	}

	@Override
	public UserSignup getUserByPhone(String phone) {
		String formattedPhone = phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
		return mapper.selectUserByPhone(formattedPhone);
	}

}
