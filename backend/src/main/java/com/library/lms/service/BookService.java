package com.library.lms.service;

import com.library.lms.exception.DatabaseOperationException;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.InvalidRequestException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return safeCall(bookRepository::findAll, "fetch all books");
    }

    public Book getBookById(String id) {
        return safeCall(() -> bookRepository.findById(id), "fetch book by id")
                .orElseThrow(() -> ResourceNotFoundException.forId("Book", id));
    }

    public List<Book> searchByTitle(String title) {
        return safeCall(() -> bookRepository.findByTitleContainingIgnoreCase(title), "search books by title");
    }

    public List<Book> searchByAuthor(String author) {
        return safeCall(() -> bookRepository.findByAuthorContainingIgnoreCase(author), "search books by author");
    }

    public List<Book> searchByGenre(String genre) {
        return safeCall(() -> bookRepository.findByGenreIgnoreCase(genre), "search books by genre");
    }

    public Book createBook(Book book) {
        boolean exists = safeCall(() -> bookRepository.existsByIsbn(book.getIsbn()), "check ISBN uniqueness");
        if (exists) {
            throw new DuplicateResourceException("A book with ISBN " + book.getIsbn() + " already exists");
        }
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw new InvalidRequestException("Available copies cannot exceed total copies");
        }
        book.setId(null); // let MongoDB generate the id
        return safeCall(() -> bookRepository.save(book), "create book");
    }

    public Book updateBook(String id, Book updated) {
        Book existing = getBookById(id);
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setGenre(updated.getGenre());
        existing.setPublishedYear(updated.getPublishedYear());
        existing.setTotalCopies(updated.getTotalCopies());
        existing.setAvailableCopies(updated.getAvailableCopies());
        if (!existing.getIsbn().equals(updated.getIsbn())) {
            boolean exists = safeCall(() -> bookRepository.existsByIsbn(updated.getIsbn()), "check ISBN uniqueness");
            if (exists) {
                throw new DuplicateResourceException("A book with ISBN " + updated.getIsbn() + " already exists");
            }
            existing.setIsbn(updated.getIsbn());
        }
        return safeCall(() -> bookRepository.save(existing), "update book");
    }

    public void deleteBook(String id) {
        getBookById(id); // ensures 404 if missing
        safeRun(() -> bookRepository.deleteById(id), "delete book");
    }

    /**
     * Decrements available copies by one. Used internally by LoanService
     * when a book is issued. Throws BookNotAvailableException (via caller)
     * if no copies remain — checked by the caller before invoking this.
     */
    Book decrementAvailableCopies(String bookId) {
        Book book = getBookById(bookId);
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        return safeCall(() -> bookRepository.save(book), "update book availability");
    }

    /** Increments available copies by one. Used when a book is returned. */
    Book incrementAvailableCopies(String bookId) {
        Book book = getBookById(bookId);
        book.setAvailableCopies(Math.min(book.getAvailableCopies() + 1, book.getTotalCopies()));
        return safeCall(() -> bookRepository.save(book), "update book availability");
    }

    // ---------------------------------------------------------------
    // Fallback helpers: every repository call funnels through here so
    // that any low-level MongoDB failure (connection refused, timeout,
    // network partition) is caught in ONE place, logged with context,
    // and surfaced to the client as a clean, predictable error instead
    // of an unhandled 500 / raw stack trace.
    // ---------------------------------------------------------------

    private <T> T safeCall(DbCall<T> call, String action) {
        try {
            return call.execute();
        } catch (DataAccessException | IllegalStateException ex) {
            log.error("Database error while trying to {}", action, ex);
            throw new DatabaseOperationException("Failed to " + action + " due to a database error", ex);
        }
    }

    private void safeRun(DbAction action, String description) {
        try {
            action.execute();
        } catch (DataAccessException | IllegalStateException ex) {
            log.error("Database error while trying to {}", description, ex);
            throw new DatabaseOperationException("Failed to " + description + " due to a database error", ex);
        }
    }

    @FunctionalInterface
    private interface DbCall<T> {
        T execute();
    }

    @FunctionalInterface
    private interface DbAction {
        void execute();
    }
}
