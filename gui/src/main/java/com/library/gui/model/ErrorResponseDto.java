package com.library.gui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Mirrors the backend's uniform ErrorResponse payload. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponseDto {
    public int status;
    public String error;
    public String message;
    public String path;
    public List<String> details;
}
