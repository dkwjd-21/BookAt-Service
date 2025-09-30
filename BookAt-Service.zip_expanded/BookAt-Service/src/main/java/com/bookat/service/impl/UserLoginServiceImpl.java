package com.bookat.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bookat.dto.UserLoginRequest;
import com.bookat.dto.UserLoginResponse;
import com.bookat.entity.User;
import com.bookat.exception.LoginException;
import com.bookat.mapper.UserLoginMapper;
import com.bookat.service.UserLoginService;
import com.bookat.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {
	
	private final UserLoginMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;
	
	// 로그인
	@Override
	public UserLoginResponse login (UserLoginRequest userLoginRequest) {
		
		User user = userMapper.findUserById(userLoginRequest.getUserId());
		
		if(user == null) {
			throw new LoginException("존재하지 않는 아이디입니다.");
		}
		
		if(!passwordEncoder.matches(userLoginRequest.getUserPw(), user.getUserPw())) {
			throw new LoginException("비밀번호가 일치하지 않습니다.");
		}
		
		String sid = UUID.randomUUID().toString();
		long ttlMs = jwtTokenProvider.getAccessTokenValidityMillis();
		String sidRedisKey = "user:" + user.getUserId() + ":current_sid";
		
		String luaScript = 
					"local old = redis.call('GET', KEYS[1]) " +
					"redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) " +
					"return old";
		
		DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(luaScript);
		redisScript.setResultType(String.class);
		
		redisTemplate.execute(redisScript, List.of(sidRedisKey), sid, String.valueOf(ttlMs));
		
		String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), sid);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
		
//		user.setRefreshToken(refreshToken);
		
		// 리프레시토큰 디비 저장
//		Map<String, String> values = new HashMap<>();
//		values.put("refreshToken", user.getRefreshToken());
//		values.put("userId", user.getUserId());
//		userMapper.updateUserRefreshToken(values);
		
		return new UserLoginResponse(accessToken, refreshToken);
	}
	
	// userId 로 사용자 조회
	@Override
	public User findUserById(String userId) {
		return userMapper.findUserById(userId);
	}
	
	// 비밀번호 변경 : 아이디와 전화번호로 사용자 조회
	@Override
	public User findPwById(String userId) {
		User user = userMapper.findUserById(userId);
		
		if(user == null) {
			throw new LoginException("존재하지 않는 아이디입니다.");
		}
		
		return user;
	}
	
	// refresh token DB에 저장
	@Override
	public void updateRefreshToken(String refreshToken, String userId) {
		Map<String, String> values = new HashMap<>();
		values.put("refreshToken", refreshToken);
		values.put("userId", userId);
		userMapper.updateUserRefreshToken(values);
	}
	
	// 비밀번호 변경
	@Override
	public void updatePassword(String password, String userId) {
		Map<String, String> values = new HashMap<>();
		values.put("password", passwordEncoder.encode(password));
		values.put("userId", userId);
		userMapper.updatePassword(values);
	}
	
	// 간편인증 후 얻은 정보로 userId 조회
	@Override
	public User findIdBySimpleAuth(String userName, String phone, String birth) {

		// '-' 없이 넘어온 전화번호에 '-' 추가시키기
		if(phone != null && phone.length() == 11) {
			phone = phone.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
		}
		
		Map<String, String> values = new HashMap<>();
		values.put("userName", userName);
		values.put("phone", phone);
		values.put("birth", birth);
		User user = userMapper.findIdBySimpleAuth(values);
		
		if(user == null) {
			throw new LoginException("회원정보가 존재하지 않습니다.");
		}
		
		return user;
	}

}
