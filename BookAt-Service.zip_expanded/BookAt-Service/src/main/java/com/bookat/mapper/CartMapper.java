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
    
    // 장바구니 추가 메서드
    void addToCart(@Param("cartId") String cartId, @Param("userId") String userId, @Param("bookId") String bookId, @Param("quantity") int quantity);
    
    // 장바구니에 이미 같은 도서가 있는지 확인
    int checkCartItem(@Param("userId") String userId, @Param("bookId") String bookId);
    
    // 기존 장바구니 아이템 수량 업데이트
    void updateExistingCartItem(@Param("userId") String userId, @Param("bookId") String bookId, @Param("quantity") int quantity);
}