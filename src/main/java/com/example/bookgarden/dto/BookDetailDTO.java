package com.example.bookgarden.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailDTO {
    private String _id;
    private String title;
    private List<CategoryDTO> categories;
    private List<AuthorDTO> authors;
    private double price;
    private int stock;
    private int soldQuantity;
    private String description;
    private String isbn;
    private String image;
    private String publisher;
    private Date publishedDate;
    private String language;
    private boolean isDeleted;
    private String pageNumbers;
    private List<ReviewDTO> reviews;
}
