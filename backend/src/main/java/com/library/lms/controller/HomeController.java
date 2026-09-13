package com.library.lms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "application", "Library Management System API",
                "status", "running",
                "endpoints", Map.of(
                        "books", "/api/books",
                        "members", "/api/members",
                        "loans", "/api/loans",
                        "health", "/actuator/health"
                )
        );
    }
}
