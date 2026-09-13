package com.library.lms.config;

import com.library.lms.model.Book;
import com.library.lms.model.Member;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds the database with a handful of sample books and members on first
 * startup so the API / GUI has data to demonstrate immediately. Only runs
 * if the collections are empty, so it is safe to leave enabled.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public DataSeeder(BookRepository bookRepository, MemberRepository memberRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (bookRepository.count() == 0) {
                bookRepository.save(new Book(null, "978-0134685991", "Effective Java", "Joshua Bloch", "Programming", 2018, 3, 3));
                bookRepository.save(new Book(null, "978-0132350884", "Clean Code", "Robert C. Martin", "Programming", 2008, 2, 2));
                bookRepository.save(new Book(null, "978-0451524935", "1984", "George Orwell", "Fiction", 1949, 4, 4));
                bookRepository.save(new Book(null, "978-0439708180", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", "Fantasy", 1997, 5, 5));
                log.info("Seeded sample books.");
            }
            if (memberRepository.count() == 0) {
                memberRepository.save(new Member(null, "Ayesha Khan", "ayesha.khan@example.com", "0300-1234567", LocalDate.now().minusMonths(6), true));
                memberRepository.save(new Member(null, "Bilal Ahmed", "bilal.ahmed@example.com", "0301-7654321", LocalDate.now().minusMonths(2), true));
                log.info("Seeded sample members.");
            }
        } catch (Exception ex) {
            // Seeding is a convenience only — never let it crash application startup.
            log.warn("Skipping data seeding: database not reachable yet ({})", ex.getMessage());
        }
    }
}
