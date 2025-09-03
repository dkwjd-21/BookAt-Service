package com.bookat.entity;

import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class Book {
  private String bookId;      // BOOK.BOOK_ID
  private String bookTitle;   // BOOK.BOOK_TITLE
  private String bookCover;   // BOOK.BOOK_COVER
  private String author;      // BOOK.AUTHOR
  private BigDecimal bookPrice; // BOOK.BOOK_PRICE
  private String publisher;   // BOOK.PUBLISHER
  private LocalDate pubdate;  // BOOK.PUBDATE
  private String description; // BOOK.DESCRIPTION
}