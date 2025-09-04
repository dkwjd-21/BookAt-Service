package com.bookat.entity;


import java.math.BigDecimal;
import java.time.LocalDate;


public class Book {
  private String bookId;      // BOOK.BOOK_ID
  private String bookTitle;   // BOOK.BOOK_TITLE
  private String bookCover;   // BOOK.BOOK_COVER
  private String author;      // BOOK.AUTHOR
  private BigDecimal bookPrice; // BOOK.BOOK_PRICE
  private String publisher;   // BOOK.PUBLISHER
  private LocalDate pubdate;  // BOOK.PUBDATE
  private String description; // BOOK.DESCRIPTION
  
  public String getBookId() {
	return bookId;
}
  public void setBookId(String bookId) {
	this.bookId = bookId;
  }
  public String getBookTitle() {
	return bookTitle;
  }
  public void setBookTitle(String bookTitle) {
	this.bookTitle = bookTitle;
  }
  public String getBookCover() {
	return bookCover;
  }
  public void setBookCover(String bookCover) {
	this.bookCover = bookCover;
  }
  public String getAuthor() {
	return author;
  }
  public void setAuthor(String author) {
	this.author = author;
  }
  public BigDecimal getBookPrice() {
	return bookPrice;
  }
  public void setBookPrice(BigDecimal bookPrice) {
	this.bookPrice = bookPrice;
  }
  public String getPublisher() {
	return publisher;
  }
  public void setPublisher(String publisher) {
	this.publisher = publisher;
  }
  public LocalDate getPubdate() {
	return pubdate;
  }
  public void setPubdate(LocalDate pubdate) {
	this.pubdate = pubdate;
  }
  public String getDescription() {
	return description;
  }
  public void setDescription(String description) {
	this.description = description;
  }

}