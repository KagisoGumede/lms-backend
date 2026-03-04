package com.example.lms.dto;

import java.time.LocalDate;

public class LeaveRequestDTO {
    public String leaveType;
    public LocalDate startDate;
    public LocalDate endDate;
    public int duration;
    public String reason;
    public Long employeeId;
}
