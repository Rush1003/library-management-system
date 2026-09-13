package com.library.lms.controller;

import com.library.lms.model.Loan;
import com.library.lms.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable String id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Loan>> getLoansByMember(@PathVariable String memberId) {
        return ResponseEntity.ok(loanService.getLoansByMember(memberId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Loan>> getOverdueLoans() {
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }

    /** Issue a book to a member. Expects query params bookId and memberId. */
    @PostMapping("/issue")
    public ResponseEntity<Loan> issueBook(@RequestParam String bookId, @RequestParam String memberId) {
        Loan loan = loanService.issueBook(bookId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    /** Return a previously issued book by loan id. */
    @PutMapping("/{id}/return")
    public ResponseEntity<Loan> returnBook(@PathVariable String id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }
}
