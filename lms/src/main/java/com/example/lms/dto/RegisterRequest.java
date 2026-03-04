package com.example.lms.dto;

public class RegisterRequest {

    public String firstName;
    public String surname;
    public String emailAddress;
    public String password;
    public String department;
    public String role;
    public Long managerId; // Optional: ID of the manager assigning this user
}
