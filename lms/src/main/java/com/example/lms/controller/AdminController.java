package com.example.lms.controller;

import com.example.lms.dto.*;
import com.example.lms.model.AuditLog;
import com.example.lms.service.AdminService;
import com.example.lms.service.AuditLogService;
import com.example.lms.service.LeaveBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    public AdminController(AdminService adminService, AuditLogService auditLogService) {
        this.adminService = adminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Stats fetched", adminService.getStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", adminService.getAllUsers()));
    }

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllManagers() {
        return ResponseEntity.ok(ApiResponse.ok("Managers fetched", adminService.getAllManagers()));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.ok("Employees fetched", adminService.getAllEmployees()));
    }

    @PostMapping("/managers")
    public ResponseEntity<ApiResponse<UserResponse>> createManager(@RequestBody RegisterRequest request) {
        UserResponse created = adminService.createManager(request);
        auditLogService.log(
            "USER_CREATED",
            "Administrator",
            "ADMIN",
            created.firstName + " " + created.surname,
            "Role: MANAGER" + (created.department != null ? " | Dept: " + created.department : "")
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Manager created successfully", created));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        String deletedName;
        String deletedRole;
        try {
            UserResponse u = adminService.getUserById(id);
            deletedName = u.firstName + " " + u.surname;
            deletedRole = u.role;
        } catch (Exception e) {
            deletedName = "Unknown (ID: " + id + ")";
            deletedRole = "UNKNOWN";
        }
        adminService.deleteManager(id);
        auditLogService.log("USER_DELETED", "Administrator", "ADMIN", deletedName, "Role: " + deletedRole);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getAllLeaves() {
        return ResponseEntity.ok(ApiResponse.ok("Leaves fetched", adminService.getAllLeaves()));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Object>> getReports() {
        return ResponseEntity.ok(ApiResponse.ok("Reports fetched", adminService.getSystemReports()));
    }

    // ─── Audit Log ───────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<AuditLog> logs = auditLogService.getAllLogs(page, size);
            return ResponseEntity.ok(Map.of(
                "success",       true,
                "data",          logs.getContent(),
                "totalPages",    logs.getTotalPages(),
                "totalElements", logs.getTotalElements(),
                "currentPage",   page
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/audit-logs/action/{action}")
    public ResponseEntity<?> getAuditLogsByAction(@PathVariable String action) {
        try {
            List<AuditLog> logs = auditLogService.getLogsByAction(action);
            return ResponseEntity.ok(Map.of("success", true, "data", logs));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─── Leave Balances ──────────────────────────────────────────

    @GetMapping("/leave-balances")
    public ResponseEntity<?> getLeaveBalances() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", leaveBalanceService.getAllBalances()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/leave-balances")
    public ResponseEntity<?> setLeaveBalance(@RequestBody Map<String, Object> body) {
        try {
            String leaveType = (String) body.get("leaveType");
            int days = (Integer) body.get("allocatedDays");
            var result = leaveBalanceService.setBalance(leaveType, days);
            auditLogService.log(
                "BALANCE_UPDATED",
                "Administrator",
                "ADMIN",
                null,
                leaveType + " set to " + days + " days"
            );
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}