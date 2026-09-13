package com.library.gui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/** Mirrors the backend's Loan document. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanDto {
    public String id;
    public String bookId;
    public String memberId;
    public LocalDate issueDate;
    public LocalDate dueDate;
    public LocalDate returnDate;
    public String status;
    public double fineAmount;
}
