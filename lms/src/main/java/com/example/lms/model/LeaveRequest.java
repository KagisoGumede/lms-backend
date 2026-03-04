                                                                                                                                                                                                                                                                                                                                                                                                                           package com.example.lms.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int duration;
    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING;

    private LocalDate submittedDate = LocalDate.now();
    private String managerComments;
    private LocalDate reviewDate;
    private String documentPath;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    // ─── Document Required Fields ────────────────────────────────
    private boolean documentRequired = false;
    private LocalDate documentDeadline;

    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus = DocumentStatus.NOT_REQUIRED;

    // ─── Getters & Setters ───────────────────────────────────────

    public Long getId() { return id; }

    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public LocalDate getSubmittedDate() { return submittedDate; }

    public String getManagerComments() { return managerComments; }
    public void setManagerComments(String managerComments) { this.managerComments = managerComments; }

    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }

    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public boolean isDocumentRequired() { return documentRequired; }
    public void setDocumentRequired(boolean documentRequired) { this.documentRequired = documentRequired; }

    public LocalDate getDocumentDeadline() { return documentDeadline; }
    public void setDocumentDeadline(LocalDate documentDeadline) { this.documentDeadline = documentDeadline; }

    public DocumentStatus getDocumentStatus() { return documentStatus; }
    public void setDocumentStatus(DocumentStatus documentStatus) { this.documentStatus = documentStatus; }
}                                                                                             