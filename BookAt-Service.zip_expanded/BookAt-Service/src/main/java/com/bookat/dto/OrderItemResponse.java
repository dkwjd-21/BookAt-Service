package com.bookat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String bookId;
    private String title;
    private String author;
    private Integer quantity;
    private Integer price;
    private String coverImage;
}
