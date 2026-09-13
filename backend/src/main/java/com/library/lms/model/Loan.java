package com.library.lms.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Represents a single loan transaction: one physical copy of a Book
 * borrowed by one Member. Stored in the "loans" MongoDB collection.
 */
@Document(collection = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    private String id;

    @NotBlank(message = "bookId is required")
    private String bookId;

    @NotBlank(message = "memberId is required")
    private String memberId;

    private LocalDate issueDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private LoanStatus status;

    private double fineAmount;
}
