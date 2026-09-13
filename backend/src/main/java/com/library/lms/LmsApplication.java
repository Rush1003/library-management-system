package com.library.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Library Management System backend.
 *
 * This Spring Boot application exposes a RESTful API backed by MongoDB
 * (a NoSQL, document-oriented database) and implements the core business
 * logic for managing books, members, and loan transactions.
 */
@SpringBootApplication
public class LmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }
}
