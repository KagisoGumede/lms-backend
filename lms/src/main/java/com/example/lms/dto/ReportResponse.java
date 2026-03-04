package com.example.lms.dto;

import java.util.List;
import java.util.Map;

public class ReportResponse {
    public int totalEmployees;
    public int totalLeaves;
    public int pendingLeaves;
    public int approvedLeaves;
    public int rejectedLeaves;
    public Map<String, Long> leavesByDepartment;
    public Map<String, Long> leavesByType;
    public Map<String, Long> leavesByMonth;
    public List<EmployeeSummary> employeeSummaries;

    public static class EmployeeSummary {
        public String employeeName;
        public String department;
        public int totalLeaves;
        public int pendingLeaves;
        public int approvedLeaves;
        public int rejectedLeaves;
    }
}