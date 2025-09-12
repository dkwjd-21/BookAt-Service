package com.bookat.mapper;

import com.bookat.entity.Book;
import com.bookat.dto.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BookMapper {
  List<Book> findNewBooks(@Param("limit") int limit);
  List<Book> findBestSellers(@Param("limit") int limit);
  List<Book> findEventBooks(@Param("limit") int limit);
  
  List<Book> findAll();
  List<Book> findByCategory(@Param("category") String category);
  List<Book> findByCategories(@Param("categories") List<String> categories);
  
  Book selectOne(@Param("bookId") String bookId);
}

