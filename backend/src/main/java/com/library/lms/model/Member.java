package com.library.lms.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Represents a library member who can borrow books.
 * Stored as a document in the "members" MongoDB collection.
 */
@Document(collection = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Indexed(unique = true)
    private String email;

    private String phone;

    private LocalDate membershipDate;

    private boolean active = true;
}
