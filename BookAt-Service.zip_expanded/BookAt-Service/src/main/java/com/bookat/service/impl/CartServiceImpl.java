package com.bookat.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
//CartServiceImpl.java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bookat.dto.CartResponse;
import com.bookat.mapper.CartMapper;
import com.bookat.service.CartService;
//... other imports

@Service
public class CartServiceImpl implements CartService {

 @Autowired
 private CartMapper cartMapper;

 @Override
 public List<CartResponse> getCartItemsForCurrentUser(String userId) {
     // 1. 현재 로그인한 사용자의 인증 정보 가져오기
     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
     
     // 2. 인증 정보에서 사용자 ID (username) 가져오기
     String currentUserId = authentication.getName();

     // 3. Mapper 메서드에 사용자 ID를 파라미터로 전달
     return cartMapper.getCartItemsForCurrentUser(userId); 
 }
}