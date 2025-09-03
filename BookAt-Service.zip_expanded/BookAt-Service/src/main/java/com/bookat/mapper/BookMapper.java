package com.bookat.mapper;

import com.bookat.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BookMapper {
  List<Book> findNewBooks(@Param("limit") int limit);
  List<Book> findBestSellers(@Param("limit") int limit);
  List<Book> findEventBooks(@Param("limit") int limit);
}