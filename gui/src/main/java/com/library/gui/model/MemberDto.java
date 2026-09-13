package com.library.gui.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/** Mirrors the backend's Member document. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDto {
    public String id;
    public String name;
    public String email;
    public String phone;
    public LocalDate membershipDate;
    public boolean active = true;

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
