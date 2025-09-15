package com.bookat.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.bookat.dto.CartResponse;

@Mapper
public interface CartMapper {
    // String 타입의 userId를 파라미터로 받도록 수정
    List<CartResponse> getCartItemsForCurrentUser(String userId); 
}