package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.dto.LeaveRequestDTO;
import com.example.lms.dto.LeaveResponse;
import com.example.lms.dto.ReportResponse;
import com.example.lms.dto.ReviewRequest;
import com.example.lms.model.LeaveRequest;
import com.example.lms.repository.LeaveRequestRepository;
import com.example.lms.service.DelegationService;
import com.example.lms.service.LeaveBalanceService;
import com.example.lms.service.LeaveService;
import com.example.lms.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveRequestRepository leaveRepo;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DelegationService delegationService;

    public LeaveController(LeaveService leaveService, LeaveRequestRepository leaveRepo) {
        this.leaveService = leaveService;
        this.leaveRepo = leaveRepo;
    }

    // ─── Leave CRUD ──────────────────────────────────────────────

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LeaveResponse>> applyLeave(@RequestBody LeaveRequestDTO dto) {
        LeaveResponse leave = leaveService.applyLeave(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Leave request submitted successfully", leave));
    }

    @GetMapping("/detail/{leaveId}")
    public ResponseEntity<ApiResponse<LeaveResponse>> getLeaveById(@PathVariable Long leaveId) {
        LeaveRequest leave = leaveRepo.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave not found"));
        return ResponseEntity.ok(ApiResponse.ok("Leave fetched", LeaveResponse.from(leave)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getMyLeaves(@PathVariable Long employeeId) {
        List<LeaveResponse> leaves = leaveService.getLeavesByEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.ok("Leave requests fetched", leaves));
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getTeamLeaves(@PathVariable Long managerId) {
        List<LeaveResponse> leaves = leaveService.getLeavesByManager(managerId);
        return ResponseEntity.ok(ApiResponse.ok("Team leave requests fetched", leaves));
    }

    @PutMapping("/{leaveId}/review")
    public ResponseEntity<ApiResponse<LeaveResponse>> reviewLeave(
            @PathVariable Long leaveId,
            @RequestBody ReviewRequest review) {
        LeaveResponse leave = leaveService.reviewLeave(leaveId, review);
        return ResponseEntity.ok(ApiResponse.ok("Leave request updated", leave));
    }

    @PutMapping("/{leaveId}/cancel")
    public ResponseEntity<ApiResponse<LeaveResponse>> cancelLeave(
            @PathVariable Long leaveId,
            @RequestBody Map<String, Long> body) {
        LeaveResponse leave = leaveService.cancelLeave(leaveId, body.get("employeeId"));
        return ResponseEntity.ok(ApiResponse.ok("Leave request cancelled", leave));
    }

    // ─── Documents ───────────────────────────────────────────────

    @PostMapping("/{leaveId}/upload")
    public ResponseEntity<ApiResponse<LeaveResponse>> uploadDocument(
            @PathVariable Long leaveId,
            @RequestParam("file") MultipartFile file) throws IOException {
        LeaveResponse leave = leaveService.uploadDocument(leaveId, file);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded successfully", leave));
    }

    @PostMapping("/{leaveId}/upload-required-doc")
    public ResponseEntity<ApiResponse<LeaveResponse>> uploadRequiredDocument(
            @PathVariable Long leaveId,
            @RequestParam("file") MultipartFile file) throws IOException {
        LeaveResponse leave = leaveService.uploadDocumentForApprovedLeave(leaveId, file);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded successfully", leave));
    }

    @PutMapping("/{leaveId}/verify-document")
    public ResponseEntity<ApiResponse<LeaveResponse>> verifyDocument(
            @PathVariable Long leaveId,
            @RequestBody Map<String, Object> body) {
        Long managerId = Long.valueOf(body.get("managerId").toString());
        boolean verified = Boolean.parseBoolean(body.get("verified").toString());
        LeaveResponse leave = leaveService.verifyDocument(leaveId, managerId, verified);
        return ResponseEntity.ok(ApiResponse.ok(
            verified ? "Document verified successfully" : "Document rejected — leave marked as UNPAID",
            leave
        ));
    }

    @GetMapping("/documents/{leaveId}")
    public ResponseEntity<Resource> getDocument(@PathVariable Long leaveId) throws IOException {
        Resource resource = leaveService.getDocument(leaveId);
        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // ─── Reports & Dashboard ─────────────────────────────────────

    @GetMapping("/reports/{managerId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReports(@PathVariable Long managerId) {
        ReportResponse report = leaveService.getReports(managerId);
        return ResponseEntity.ok(ApiResponse.ok("Reports fetched", report));
    }

    @GetMapping("/dashboard/{managerId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(@PathVariable Long managerId) {
        Map<String, Object> stats = leaveService.getDashboardStats(managerId);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard stats fetched", stats));
    }

    // ─── Leave Balances ──────────────────────────────────────────

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<?> getEmployeeBalances(@PathVariable Long employeeId) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", leaveBalanceService.getEmployeeBalances(employeeId)
        ));
    }

    // ─── Notifications ───────────────────────────────────────────

    @GetMapping("/notifications/{userId}")
    public ResponseEntity<?> getNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of(
            "success",     true,
            "data",        notificationService.getForUser(userId),
            "unreadCount", notificationService.getUnreadCount(userId)
        ));
    }

    @PutMapping("/notifications/{userId}/read-all")
    public ResponseEntity<?> markAllRead(@PathVariable Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<?> markOneRead(@PathVariable Long notificationId) {
        notificationService.markOneRead(notificationId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ─── Delegation ──────────────────────────────────────────────

    @PostMapping("/delegate")
    public ResponseEntity<?> setDelegation(@RequestBody Map<String, Object> body) {
        try {
            Long delegatorId = Long.valueOf(body.get("delegatorId").toString());
            Long delegateId  = Long.valueOf(body.get("delegateId").toString());
            LocalDate expiry = LocalDate.parse(body.get("expiryDate").toString());
            var result = delegationService.delegate(delegatorId, delegateId, expiry);
            return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
                "id",           result.getId(),
                "delegateId",   result.getDelegate().getId(),
                "delegateName", result.getDelegate().getFirstName() + " " + result.getDelegate().getSurname(),
                "expiryDate",   result.getExpiryDate().toString(),
                "active",       result.isActive()
            )));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/delegate/{delegatorId}")
    public ResponseEntity<?> removeDelegation(@PathVariable Long delegatorId) {
        try {
            delegationService.removeDelegation(delegatorId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/delegate/{managerId}")
    public ResponseEntity<?> getDelegation(@PathVariable Long managerId) {
        try {
            var active = delegationService.getActiveDelegation(managerId);
            var delegatedToMe = delegationService.getDelegatedToMe(managerId);

            Map<String, Object> activeDelegation = active.map(d -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           d.getId());
                m.put("delegateId",   d.getDelegate().getId());
                m.put("delegateName", d.getDelegate().getFirstName() + " " + d.getDelegate().getSurname());
                m.put("expiryDate",   d.getExpiryDate().toString());
                return m;
            }).orElse(null);

            List<Map<String, Object>> incoming = delegatedToMe.stream().map(d -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",            d.getId());
                m.put("delegatorId",   d.getDelegator().getId());
                m.put("delegatorName", d.getDelegator().getFirstName() + " " + d.getDelegator().getSurname());
                m.put("expiryDate",    d.getExpiryDate().toString());
                return m;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success",          true,
                "activeDelegation", activeDelegation != null ? activeDelegation : Map.of(),
                "delegatedToMe",    incoming
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}