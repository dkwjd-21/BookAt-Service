package com.bookat.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {
    private String bookId;     // BOOK.book_id
    private String title;      // BOOK.book_title
    private String author;     // BOOK.author
    private String publisher;  // BOOK.publisher
    private BigDecimal price;     // BOOK.book_price
    private String imageUrl;   // BOOK.book_cover
    private String category;   // BOOK.category
}