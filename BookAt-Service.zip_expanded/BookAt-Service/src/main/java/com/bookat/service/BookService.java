package com.bookat.service;

import com.bookat.dto.*;

import java.util.List;

public interface BookService {
	
  List<BookListRes> getBestSellers(int limit);
  List<BookListRes> getNewBooks(int limit);
  List<BookListRes> getEventBooks(int limit);

  List<BookDto> findAll();
  List<BookDto> findByCategory(String category);
  List<BookDto> findByCategories(List<String> categories);
  
  //단건 조회(상세페이지)
  BookDto selectOne(String bookId);
}