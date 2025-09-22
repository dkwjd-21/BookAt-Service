package com.bookat.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.bookat.dto.CartResponse;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartMapper {
    // String 타입의 userId를 파라미터로 받도록 수정
    List<CartResponse> getCartItemsForCurrentUser(String userId);
    
    // 주문용 메서드들 추가
    List<CartResponse> getCartItemsByIds(List<String> cartIds);
    void deleteCartItems(List<String> cartIds);

    void deleteCartItem(String cartId);

    void updateCartItemQuantity(@Param("cartId") String cartId, @Param("quantity") int quantity);
}