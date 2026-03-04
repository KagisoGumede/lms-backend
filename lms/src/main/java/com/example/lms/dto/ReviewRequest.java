package com.example.lms.dto;


public class ReviewRequest {
    public Long managerId;
    public String status;
    public String managerComments;

    // ─── Document Required Fields ────────────────────────────────
    public boolean documentRequired;
    public String documentDeadline; // ISO date string e.g. "2025-03-01"
}