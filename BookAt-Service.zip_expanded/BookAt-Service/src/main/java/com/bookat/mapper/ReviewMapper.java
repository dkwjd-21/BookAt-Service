package com.bookat.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import com.bookat.dto.ReviewDto;

@Mapper
public interface ReviewMapper {
    List<ReviewDto> findByBookId(@Param("bookId") String bookId);
    int countByBookId(@Param("bookId") String bookId);
}