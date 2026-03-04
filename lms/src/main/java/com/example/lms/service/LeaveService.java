package com.example.lms.service;

import com.example.lms.dto.LeaveRequestDTO;
import com.example.lms.dto.LeaveResponse;
import com.example.lms.dto.ReportResponse;
import com.example.lms.dto.ReviewRequest;
import com.example.lms.model.DocumentStatus;
import com.example.lms.model.LeaveRequest;
import com.example.lms.model.LeaveStatus;
import com.example.lms.model.User;
import com.example.lms.repository.LeaveRequestRepository;
import com.example.lms.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRepo;
    private final UserRepository userRepo;
    private final FileStorageService fileStorageService;
    private final EmailNotificationService emailService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final DelegationService delegationService;
    private final TeamsNotificationService teamsService;

    public LeaveService(LeaveRequestRepository leaveRepo,
                        UserRepository userRepo,
                        FileStorageService fileStorageService,
                        EmailNotificationService emailService,
                        AuditLogService auditLogService,
                        NotificationService notificationService,
                        @Lazy DelegationService delegationService,
                        TeamsNotificationService teamsService) {
        this.leaveRepo = leaveRepo;
        this.userRepo = userRepo;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.delegationService = delegationService;
        this.teamsService = teamsService;
    }

    // ─── Apply Leave ─────────────────────────────────────────────

    public LeaveResponse applyLeave(LeaveRequestDTO dto) {
        User employee = userRepo.findById(dto.employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (dto.leaveType == null || dto.leaveType.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave type is required");
        if (dto.startDate == null || dto.endDate == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end date are required");
        if (dto.endDate.isBefore(dto.startDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        if (dto.reason == null || dto.reason.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason is required");

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(dto.leaveType);
        leave.setStartDate(dto.startDate);
        leave.setEndDate(dto.endDate);
        leave.setDuration(dto.duration);
        leave.setReason(dto.reason);

        leaveRepo.save(leave);

        auditLogService.log(
            "LEAVE_APPLIED",
            employee.getFirstName() + " " + employee.getSurname(),
            employee.getRole().name(),
            null,
            dto.leaveType + " from " + dto.startDate + " to " + dto.endDate + " (" + dto.duration + " days)"
        );

        // ─── Notify manager OR admin if applicant is a MANAGER ───
        String managerName;
        if (employee.getManager() != null) {
            managerName = employee.getManager().getFirstName() + " " + employee.getManager().getSurname();
            notificationService.send(
                employee.getManager().getId(),
                "New Leave Request",
                employee.getFirstName() + " " + employee.getSurname()
                    + " applied for " + dto.leaveType
                    + " (" + dto.duration + " days)",
                "LEAVE_APPLIED"
            );
        } else {
            managerName = "Admin";
            userRepo.findAll().stream()
                .filter(u -> u.getRole().name().equals("ADMIN"))
                .forEach(admin -> notificationService.send(
                    admin.getId(),
                    "Manager Leave Request",
                    employee.getFirstName() + " " + employee.getSurname()
                        + " (Manager) applied for " + dto.leaveType
                        + " (" + dto.duration + " days). Requires your approval.",
                    "LEAVE_APPLIED"
                ));
        }

        emailService.sendLeaveSubmitted(leave);

        // ─── Notify Teams ─────────────────────────────────────────
        teamsService.notifyLeaveSubmitted(
            employee.getFirstName() + " " + employee.getSurname(),
            dto.leaveType,
            dto.startDate.toString(),
            dto.endDate.toString(),
            managerName
        );

        return LeaveResponse.from(leave);
    }

    // ─── Get Leaves ──────────────────────────────────────────────

    public List<LeaveResponse> getLeavesByEmployee(Long employeeId) {
        return leaveRepo.findByEmployeeId(employeeId)
                .stream().map(LeaveResponse::from).collect(Collectors.toList());
    }

    public List<LeaveResponse> getLeavesByManager(Long managerId) {
        List<LeaveRequest> leaves = new ArrayList<>(leaveRepo.findByEmployeeManagerId(managerId));

        delegationService.getDelegatedToMe(managerId).forEach(delegation ->
            leaves.addAll(leaveRepo.findByEmployeeManagerId(delegation.getDelegator().getId()))
        );

        return leaves.stream().map(LeaveResponse::from).collect(Collectors.toList());
    }

    // ─── Review Leave ────────────────────────────────────────────

    public LeaveResponse reviewLeave(Long leaveId, ReviewRequest review) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        User manager = userRepo.findById(review.managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));

        LeaveStatus newStatus;
        try {
            newStatus = LeaveStatus.valueOf(review.status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be APPROVED or REJECTED");
        }

        leave.setStatus(newStatus);
        leave.setManagerComments(review.managerComments);
        leave.setReviewedBy(manager);
        leave.setReviewDate(LocalDate.now());

        // ─── Handle optional document required ───────────────────
        if (newStatus == LeaveStatus.APPROVED && review.documentRequired) {
            leave.setDocumentRequired(true);
            leave.setDocumentStatus(DocumentStatus.PENDING);
            if (review.documentDeadline != null && !review.documentDeadline.isBlank()) {
                leave.setDocumentDeadline(LocalDate.parse(review.documentDeadline));
            } else {
                leave.setDocumentDeadline(leave.getEndDate().plusDays(7));
            }
        } else {
            leave.setDocumentRequired(false);
            leave.setDocumentStatus(DocumentStatus.NOT_REQUIRED);
        }

        leaveRepo.save(leave);

        String employeeName = leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname();
        String managerName  = manager.getFirstName() + " " + manager.getSurname();

        auditLogService.log(
            newStatus == LeaveStatus.APPROVED ? "LEAVE_APPROVED" : "LEAVE_REJECTED",
            managerName,
            manager.getRole().name(),
            employeeName,
            leave.getLeaveType() + " — " + leave.getDuration() + " days"
                + (review.managerComments != null && !review.managerComments.isBlank()
                    ? " | Comment: " + review.managerComments : "")
                + (review.documentRequired
                    ? " | Document required by " + leave.getDocumentDeadline() : "")
        );

        // ─── Build notification message ───────────────────────────
        String notifMessage = "Your " + leave.getLeaveType() + " request has been "
                + newStatus.name().toLowerCase()
                + (review.managerComments != null && !review.managerComments.isBlank()
                    ? ". Comment: " + review.managerComments : "");

        if (newStatus == LeaveStatus.APPROVED && review.documentRequired) {
            notifMessage += ". You must upload a supporting document by "
                    + leave.getDocumentDeadline()
                    + ". Failure to do so will result in the leave being marked as UNPAID.";
        }

        notificationService.send(
            leave.getEmployee().getId(),
            newStatus == LeaveStatus.APPROVED ? "Leave Approved" : "Leave Rejected",
            notifMessage,
            newStatus == LeaveStatus.APPROVED ? "LEAVE_APPROVED" : "LEAVE_REJECTED"
        );

        if (newStatus == LeaveStatus.APPROVED) {
            emailService.sendLeaveApproved(leave);
            // ─── Notify Teams: Approved ───────────────────────────
            teamsService.notifyLeaveApproved(
                employeeName,
                leave.getLeaveType(),
                leave.getStartDate().toString(),
                leave.getEndDate().toString(),
                managerName
            );
        } else if (newStatus == LeaveStatus.REJECTED) {
            emailService.sendLeaveRejected(leave);
            // ─── Notify Teams: Rejected ───────────────────────────
            teamsService.notifyLeaveRejected(
                employeeName,
                leave.getLeaveType(),
                leave.getStartDate().toString(),
                leave.getEndDate().toString(),
                managerName
            );
        }

        return LeaveResponse.from(leave);
    }

    // ─── Cancel Leave ────────────────────────────────────────────

    public LeaveResponse cancelLeave(Long leaveId, Long employeeId) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (!leave.getEmployee().getId().equals(employeeId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only cancel your own leave requests");

        if (leave.getStatus() != LeaveStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only pending leave requests can be cancelled");

        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepo.save(leave);

        auditLogService.log(
            "LEAVE_CANCELLED",
            leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname(),
            leave.getEmployee().getRole().name(),
            null,
            leave.getLeaveType() + " from " + leave.getStartDate() + " to " + leave.getEndDate()
                + " (" + leave.getDuration() + " days)"
        );

        // ─── Notify manager OR admins if canceller is a MANAGER ──
        if (leave.getEmployee().getManager() != null) {
            notificationService.send(
                leave.getEmployee().getManager().getId(),
                "Leave Cancelled",
                leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname()
                    + " cancelled their " + leave.getLeaveType() + " request",
                "LEAVE_CANCELLED"
            );
        } else {
            userRepo.findAll().stream()
                .filter(u -> u.getRole().name().equals("ADMIN"))
                .forEach(admin -> notificationService.send(
                    admin.getId(),
                    "Manager Leave Cancelled",
                    leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname()
                        + " (Manager) cancelled their " + leave.getLeaveType() + " request",
                    "LEAVE_CANCELLED"
                ));
        }

        return LeaveResponse.from(leave);
    }

    // ─── Upload Document (at apply time) ─────────────────────────

    public LeaveResponse uploadDocument(Long leaveId, MultipartFile file) throws IOException {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));

        String path = fileStorageService.saveFile(file, leaveId);
        leave.setDocumentPath(path);
        leaveRepo.save(leave);
        return LeaveResponse.from(leave);
    }

    // ─── Upload Required Document (after approval) ───────────────

    public LeaveResponse uploadDocumentForApprovedLeave(Long leaveId, MultipartFile file) throws IOException {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));

        if (!leave.isDocumentRequired())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No document required for this leave");

        if (leave.getDocumentStatus() == DocumentStatus.VERIFIED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Document already verified");

        String path = fileStorageService.saveFile(file, leaveId);
        leave.setDocumentPath(path);
        leave.setDocumentStatus(DocumentStatus.UPLOADED);
        leaveRepo.save(leave);

        auditLogService.log(
            "DOCUMENT_UPLOADED",
            leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname(),
            leave.getEmployee().getRole().name(),
            null,
            leave.getLeaveType() + " leave #" + leaveId
        );

        // ─── Notify manager OR admins ─────────────────────────────
        if (leave.getEmployee().getManager() != null) {
            notificationService.send(
                leave.getEmployee().getManager().getId(),
                "Document Uploaded",
                leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname()
                    + " uploaded a supporting document for their "
                    + leave.getLeaveType() + " leave. Please verify.",
                "DOCUMENT_UPLOADED"
            );
        } else {
            userRepo.findAll().stream()
                .filter(u -> u.getRole().name().equals("ADMIN"))
                .forEach(admin -> notificationService.send(
                    admin.getId(),
                    "Manager Document Uploaded",
                    leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname()
                        + " (Manager) uploaded a supporting document for their "
                        + leave.getLeaveType() + " leave. Please verify.",
                    "DOCUMENT_UPLOADED"
                ));
        }

        return LeaveResponse.from(leave);
    }

    // ─── Verify Document ─────────────────────────────────────────

    public LeaveResponse verifyDocument(Long leaveId, Long managerId, boolean verified) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));

        User manager = userRepo.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));

        if (leave.getDocumentStatus() != DocumentStatus.UPLOADED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No uploaded document to verify");

        if (verified) {
            leave.setDocumentStatus(DocumentStatus.VERIFIED);

            auditLogService.log(
                "DOCUMENT_VERIFIED",
                manager.getFirstName() + " " + manager.getSurname(),
                manager.getRole().name(),
                leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname(),
                leave.getLeaveType() + " leave #" + leaveId
            );

            notificationService.send(
                leave.getEmployee().getId(),
                "Document Verified",
                "Your supporting document for " + leave.getLeaveType()
                    + " leave has been verified. Your leave remains approved.",
                "DOCUMENT_VERIFIED"
            );

        } else {
            leave.setDocumentStatus(DocumentStatus.REJECTED);
            leave.setStatus(LeaveStatus.UNPAID);

            auditLogService.log(
                "DOCUMENT_REJECTED",
                manager.getFirstName() + " " + manager.getSurname(),
                manager.getRole().name(),
                leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname(),
                leave.getLeaveType() + " leave #" + leaveId + " — converted to UNPAID"
            );

            notificationService.send(
                leave.getEmployee().getId(),
                "Document Rejected — Leave Marked as Unpaid",
                "Your supporting document for " + leave.getLeaveType()
                    + " leave was rejected. Your leave has been marked as UNPAID.",
                "DOCUMENT_REJECTED"
            );
        }

        leaveRepo.save(leave);
        return LeaveResponse.from(leave);
    }

    // ─── Get Document ────────────────────────────────────────────

    public org.springframework.core.io.Resource getDocument(Long leaveId) throws IOException {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));

        if (leave.getDocumentPath() == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No document found for this leave request");

        Path filePath = fileStorageService.getFile(leave.getDocumentPath());
        org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found on server");

        return resource;
    }

    // ─── Scheduled: Auto-mark overdue documents as UNPAID ────────

    @Scheduled(cron = "0 0 0 * * *")
    public void autoMarkOverdueDocumentsAsUnpaid() {
        List<LeaveRequest> overdue = leaveRepo.findOverdueDocuments(LocalDate.now());
        overdue.forEach(leave -> {
            leave.setStatus(LeaveStatus.UNPAID);
            leave.setDocumentStatus(DocumentStatus.REJECTED);
            leaveRepo.save(leave);

            auditLogService.log(
                "LEAVE_UNPAID",
                "System",
                "SYSTEM",
                leave.getEmployee().getFirstName() + " " + leave.getEmployee().getSurname(),
                leave.getLeaveType() + " — document deadline passed"
            );

            notificationService.send(
                leave.getEmployee().getId(),
                "Leave Marked as Unpaid",
                "Your " + leave.getLeaveType() + " leave has been marked as UNPAID because "
                    + "you did not upload a supporting document by the deadline ("
                    + leave.getDocumentDeadline() + ").",
                "LEAVE_UNPAID"
            );
        });
    }

    // ─── REPORTS ─────────────────────────────────────────────────

    public ReportResponse getReports(Long managerId) {
        List<LeaveRequest> leaves = leaveRepo.findByEmployeeManagerId(managerId);
        List<User> employees = userRepo.findByManagerId(managerId);

        ReportResponse report = new ReportResponse();
        report.totalEmployees = employees.size();
        report.totalLeaves = leaves.size();
        report.pendingLeaves = (int) leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
        report.approvedLeaves = (int) leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        report.rejectedLeaves = (int) leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();

        report.leavesByDepartment = leaves.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getEmployee().getDepartment(),
                        Collectors.counting()
                ));

        report.leavesByType = leaves.stream()
                .collect(Collectors.groupingBy(
                        LeaveRequest::getLeaveType,
                        Collectors.counting()
                ));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, Long> leavesByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            leavesByMonth.put(LocalDate.now().minusMonths(i).format(formatter), 0L);
        }
        leaves.forEach(l -> {
            String key = l.getStartDate().format(formatter);
            if (leavesByMonth.containsKey(key))
                leavesByMonth.put(key, leavesByMonth.get(key) + 1);
        });
        report.leavesByMonth = leavesByMonth;

        report.employeeSummaries = employees.stream().map(emp -> {
            ReportResponse.EmployeeSummary summary = new ReportResponse.EmployeeSummary();
            summary.employeeName = emp.getFirstName() + " " + emp.getSurname();
            summary.department = emp.getDepartment();
            List<LeaveRequest> empLeaves = leaves.stream()
                    .filter(l -> l.getEmployee().getId().equals(emp.getId()))
                    .collect(Collectors.toList());
            summary.totalLeaves = empLeaves.size();
            summary.pendingLeaves = (int) empLeaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
            summary.approvedLeaves = (int) empLeaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
            summary.rejectedLeaves = (int) empLeaves.stream()
                    .filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();
            return summary;
        }).collect(Collectors.toList());

        return report;
    }

    // ─── DASHBOARD STATS ─────────────────────────────────────────

    public Map<String, Object> getDashboardStats(Long managerId) {
        List<LeaveRequest> leaves = leaveRepo.findByEmployeeManagerId(managerId);
        List<User> employees = userRepo.findByManagerId(managerId);

        long approvedThisMonth = leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> l.getStartDate().getMonth() == LocalDate.now().getMonth()
                        && l.getStartDate().getYear() == LocalDate.now().getYear())
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEmployees", employees.size());
        stats.put("pendingLeaves", leaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.PENDING).count());
        stats.put("approvedThisMonth", approvedThisMonth);
        stats.put("totalLeaves", leaves.size());

        return stats;
    }
}