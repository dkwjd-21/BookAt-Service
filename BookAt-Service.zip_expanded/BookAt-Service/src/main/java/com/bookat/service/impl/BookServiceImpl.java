package com.bookat.service.impl;

import com.bookat.dto.BookListRes;
import com.bookat.entity.Book;
import com.bookat.mapper.BookMapper;
import com.bookat.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookMapper bookMapper;

  private static BookListRes toRes(Book b){
    String img = (b.getBookCover()==null || b.getBookCover().isBlank())
      ? "https://placehold.co/300x420?text=Book" : b.getBookCover();
    return new BookListRes(
      b.getBookId(),
      b.getBookTitle(),
      b.getAuthor(),
      b.getBookPrice(),
      img
    );
  }

  @Override public List<BookListRes> getBestSellers(int limit){
    return bookMapper.findBestSellers(limit).stream().map(BookServiceImpl::toRes).collect(Collectors.toList());
  }
  @Override public List<BookListRes> getNewBooks(int limit){
    return bookMapper.findNewBooks(limit).stream().map(BookServiceImpl::toRes).collect(Collectors.toList());
  }
  @Override public List<BookListRes> getEventBooks(int limit){
    return bookMapper.findEventBooks(limit).stream().map(BookServiceImpl::toRes).collect(Collectors.toList());
  }
}