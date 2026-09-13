package com.library.lms.service;

import com.library.lms.exception.BookNotAvailableException;
import com.library.lms.exception.DatabaseOperationException;
import com.library.lms.exception.InvalidRequestException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.model.Loan;
import com.library.lms.model.LoanStatus;
import com.library.lms.model.Member;
import com.library.lms.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Encapsulates the core library workflow: issuing a book to a member and
 * processing its return (including automatic overdue-fine calculation).
 */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final BookService bookService;
    private final MemberService memberService;

    @Value("${library.loan.default-period-days:14}")
    private int loanPeriodDays;

    @Value("${library.loan.fine-per-day:0.50}")
    private double finePerDay;

    public LoanService(LoanRepository loanRepository, BookService bookService, MemberService memberService) {
        this.loanRepository = loanRepository;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    public List<Loan> getAllLoans() {
        return safeCall(loanRepository::findAll, "fetch all loans");
    }

    public Loan getLoanById(String id) {
        return safeCall(() -> loanRepository.findById(id), "fetch loan by id")
                .orElseThrow(() -> ResourceNotFoundException.forId("Loan", id));
    }

    public List<Loan> getLoansByMember(String memberId) {
        memberService.getMemberById(memberId); // 404 if member doesn't exist
        return safeCall(() -> loanRepository.findByMemberId(memberId), "fetch loans by member");
    }

    public List<Loan> getOverdueLoans() {
        List<Loan> issued = safeCall(() -> loanRepository.findByStatus(LoanStatus.ISSUED), "fetch issued loans");
        LocalDate today = LocalDate.now();
        return issued.stream().filter(l -> l.getDueDate().isBefore(today)).toList();
    }

    /**
     * Issues a book to a member: validates both exist, checks copy
     * availability, decrements the book's available count, and records a
     * new Loan with a due date {@code loanPeriodDays} from today.
     */
    public Loan issueBook(String bookId, String memberId) {
        Book book = bookService.getBookById(bookId);
        Member member = memberService.getMemberById(memberId);

        if (!member.isActive()) {
            throw new InvalidRequestException("Member " + memberId + " is not active and cannot borrow books");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BookNotAvailableException("No available copies of '" + book.getTitle() + "' to issue");
        }

        bookService.decrementAvailableCopies(bookId);

        Loan loan = new Loan();
        loan.setBookId(bookId);
        loan.setMemberId(memberId);
        loan.setIssueDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(loanPeriodDays));
        loan.setStatus(LoanStatus.ISSUED);
        loan.setFineAmount(0.0);

        return safeCall(() -> loanRepository.save(loan), "create loan");
    }

    /**
     * Marks a loan as returned, restores the book's available copy count,
     * and calculates a late fine if the return happens after the due date.
     */
    public Loan returnBook(String loanId) {
        Loan loan = getLoanById(loanId);

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new InvalidRequestException("Loan " + loanId + " has already been returned");
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);
        loan.setStatus(LoanStatus.RETURNED);

        long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
        if (overdueDays > 0) {
            loan.setFineAmount(overdueDays * finePerDay);
        } else {
            loan.setFineAmount(0.0);
        }

        bookService.incrementAvailableCopies(loan.getBookId());

        return safeCall(() -> loanRepository.save(loan), "update loan on return");
    }

    private <T> T safeCall(DbCall<T> call, String action) {
        try {
            return call.execute();
        } catch (DataAccessException | IllegalStateException ex) {
            log.error("Database error while trying to {}", action, ex);
            throw new DatabaseOperationException("Failed to " + action + " due to a database error", ex);
        }
    }

    @FunctionalInterface
    private interface DbCall<T> {
        T execute();
    }
}
