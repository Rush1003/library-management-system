package com.library.gui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors the backend's Book document. Field names must match the JSON keys. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookDto {
    public String id;
    public String isbn;
    public String title;
    public String author;
    public String genre;
    public int publishedYear;
    public int totalCopies;
    public int availableCopies;

    @Override
    public String toString() {
        return title + " (" + isbn + ")";
    }
}
