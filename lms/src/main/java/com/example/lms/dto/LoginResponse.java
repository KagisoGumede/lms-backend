package com.example.lms.dto;

public class LoginResponse {
    public Long id;
    public String firstName;
    public String surname;
    public String emailAddress;
    public String department;
    public String role;

    public LoginResponse(Long id, String firstName, String surname,
                         String emailAddress, String department, String role) {
        this.id = id;
        this.firstName = firstName;
        this.surname = surname;
        this.emailAddress = emailAddress;
        this.department = department;
        this.role = role;
    }
}