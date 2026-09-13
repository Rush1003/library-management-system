package com.library.lms.service;

import com.library.lms.exception.DatabaseOperationException;
import com.library.lms.exception.DuplicateResourceException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.model.Book;
import com.library.lms.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests covering: successful CRUD, business-rule validation (duplicate
 * ISBN), not-found handling, and the database-failure fallback path.
 */
class BookServiceTest {

    private BookRepository bookRepository;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookRepository = Mockito.mock(BookRepository.class);
        bookService = new BookService(bookRepository);
    }

    @Test
    void getBookById_returnsBook_whenFound() {
        Book book = new Book(null, "978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 3, 3);
        when(bookRepository.findById("abc")).thenReturn(Optional.of(book));

        Book result = bookService.getBookById("abc");

        assertEquals("Effective Java", result.getTitle());
    }

    @Test
    void getBookById_throwsNotFound_whenMissing() {
        when(bookRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById("missing"));
    }

    @Test
    void createBook_throwsDuplicate_whenIsbnAlreadyExists() {
        Book book = new Book(null, "978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 3, 3);
        when(bookRepository.existsByIsbn("978-0134685991")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> bookService.createBook(book));
    }

    @Test
    void createBook_savesSuccessfully_whenIsbnIsUnique() {
        Book book = new Book(null, "978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 3, 3);
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.createBook(book);

        assertEquals("Effective Java", result.getTitle());
        Mockito.verify(bookRepository).save(any(Book.class));
    }

    @Test
    void getAllBooks_wrapsDatabaseFailure_asDatabaseOperationException() {
        when(bookRepository.findAll()).thenThrow(new DataAccessResourceFailureException("Mongo connection refused"));

        assertThrows(DatabaseOperationException.class, () -> bookService.getAllBooks());
    }
}
