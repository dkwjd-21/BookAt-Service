package com.bookat.service;

import com.bookat.dto.BookListRes;
import java.util.List;

public interface BookService {
  List<BookListRes> getBestSellers(int limit);
  List<BookListRes> getNewBooks(int limit);
  List<BookListRes> getEventBooks(int limit);
}