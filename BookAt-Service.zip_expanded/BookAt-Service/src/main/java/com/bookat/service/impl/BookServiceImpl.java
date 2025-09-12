package com.bookat.service.impl;

import com.bookat.dto.BookListRes;
import com.bookat.entity.Book;
import com.bookat.mapper.BookMapper;
import com.bookat.service.*;
import com.bookat.dto.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

  //메인
  @Override public List<BookListRes> getBestSellers(int limit){
	  return bookMapper.findBestSellers(limit).stream().map(BookServiceImpl::toRes).toList();
	}
	@Override public List<BookListRes> getNewBooks(int limit){
	  return bookMapper.findNewBooks(limit).stream().map(BookServiceImpl::toRes).toList();
	}
	@Override public List<BookListRes> getEventBooks(int limit){
	  return bookMapper.findEventBooks(limit).stream().map(BookServiceImpl::toRes).toList();
	}
  
  //상세페이지
  @Override
  public BookDto selectOne(String bookId) {
	  Book b = bookMapper.selectOne(bookId);
	    return BookDto.builder()
	            .bookId(b.getBookId())
	            .title(b.getBookTitle())
	            .author(b.getAuthor())
	            .publisher(b.getPublisher())
	            .price(b.getBookPrice())
	            .imageUrl(b.getBookCover())   
	            .category(b.getCategory())
	            .pubdate(b.getPubdate())
	            .description(b.getDescription())
	            .build();
  }
  
  
  // 카테고리 도서 조회
  private static BookDto toDto(Book b){
	    return BookDto.builder()
	      .bookId(b.getBookId())
	      .title(b.getBookTitle())
	      .author(b.getAuthor())
	      .publisher(b.getPublisher())
	      .price(b.getBookPrice())   
	      .imageUrl(b.getBookCover())
	      .category(b.getCategory())
	      .build();
	  }
  
  @Override public List<BookDto> findAll(){
	    return bookMapper.findAll().stream().map(BookServiceImpl::toDto).collect(Collectors.toList());
	  }
	  @Override public List<BookDto> findByCategory(String category){
	    return bookMapper.findByCategory(category).stream().map(BookServiceImpl::toDto).collect(Collectors.toList());
	  }
	  @Override public List<BookDto> findByCategories(List<String> categories){
	    return bookMapper.findByCategories(categories).stream().map(BookServiceImpl::toDto).collect(Collectors.toList());
	  }
  
}