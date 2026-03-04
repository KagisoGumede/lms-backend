package com.example.lms.dto;

import com.example.lms.model.LeaveRequest;
import java.time.LocalDate;

public class LeaveResponse {

    public Long id;
    public String leaveType;
    public LocalDate startDate;
    public LocalDate endDate;
    public int duration;
    public String reason;
    public String status;
    public LocalDate submittedDate;
    public String managerComments;
    public LocalDate reviewDate;
    public String employeeName;
    public String department;
    public Long employeeId;
    public String employeeRole;        // ← NEW
    public String reviewedByName;
    public String documentUrl;

    // ─── Document Required Fields ────────────────────────────────
    public boolean documentRequired;
    public LocalDate documentDeadline;
    public String documentStatus;

    public static LeaveResponse from(LeaveRequest lr) {
        LeaveResponse res = new LeaveResponse();
        res.id              = lr.getId();
        res.leaveType       = lr.getLeaveType();
        res.startDate       = lr.getStartDate();
        res.endDate         = lr.getEndDate();
        res.duration        = lr.getDuration();
        res.reason          = lr.getReason();
        res.status          = lr.getStatus().name();
        res.submittedDate   = lr.getSubmittedDate();
        res.managerComments = lr.getManagerComments();
        res.reviewDate      = lr.getReviewDate();
        res.employeeId      = lr.getEmployee().getId();
        res.employeeName    = lr.getEmployee().getFirstName() + " " + lr.getEmployee().getSurname();
        res.department      = lr.getEmployee().getDepartment();
        res.employeeRole    = lr.getEmployee().getRole().name(); // ← NEW
        res.documentUrl     = lr.getDocumentPath() != null
                ? "/api/leaves/documents/" + lr.getId()
                : null;
        if (lr.getReviewedBy() != null)
            res.reviewedByName = lr.getReviewedBy().getFirstName() + " " + lr.getReviewedBy().getSurname();

        res.documentRequired = lr.isDocumentRequired();
        res.documentDeadline = lr.getDocumentDeadline();
        res.documentStatus   = lr.getDocumentStatus().name();

        return res;
    }
}