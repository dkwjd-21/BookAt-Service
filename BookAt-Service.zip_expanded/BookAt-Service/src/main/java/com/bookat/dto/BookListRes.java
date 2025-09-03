package com.bookat.dto;

import lombok.AllArgsConstructor; import lombok.Data;
import java.math.BigDecimal;

@Data @AllArgsConstructor
public class BookListRes {
  private String bookId;
  private String title;     // = entity.bookTitle 매핑
  private String author;
  private BigDecimal price; // = entity.bookPrice 매핑
  private String imageUrl;  // = entity.bookCover 매핑
}