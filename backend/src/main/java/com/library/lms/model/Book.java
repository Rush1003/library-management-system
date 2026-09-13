package com.library.lms.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a book title held by the library.
 * Stored as a document in the "books" MongoDB collection.
 *
 * Note: a single Book document represents a *title*; totalCopies /
 * availableCopies track how many physical copies of that title exist
 * and how many are currently on the shelf (not on loan).
 */
@Document(collection = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    private String id;

    @NotBlank(message = "ISBN is required")
    @Indexed(unique = true)
    private String isbn;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String genre;

    @Min(value = 0, message = "Published year must be a positive number")
    private int publishedYear;

    @Min(value = 0, message = "Total copies cannot be negative")
    private int totalCopies;

    @Min(value = 0, message = "Available copies cannot be negative")
    private int availableCopies;
}
