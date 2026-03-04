package com.example.lms.service;

import com.example.lms.dto.AdminStatsResponse;
import com.example.lms.dto.LeaveResponse;
import com.example.lms.dto.RegisterRequest;
import com.example.lms.dto.UserResponse;
import com.example.lms.model.LeaveStatus;
import com.example.lms.model.Role;
import com.example.lms.model.User;
import com.example.lms.repository.LeaveRequestRepository;
import com.example.lms.repository.LeaveDelegationRepository;
import com.example.lms.repository.NotificationRepository;
import com.example.lms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepo;
    private final LeaveRequestRepository leaveRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailNotificationService emailService;
    private final LeaveDelegationRepository delegationRepo;
    private final NotificationRepository notificationRepo;

    public AdminService(UserRepository userRepo,
                        LeaveRequestRepository leaveRepo,
                        BCryptPasswordEncoder passwordEncoder,
                        EmailNotificationService emailService,
                        LeaveDelegationRepository delegationRepo,
                        NotificationRepository notificationRepo) {
        this.userRepo = userRepo;
        this.leaveRepo = leaveRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.delegationRepo = delegationRepo;
        this.notificationRepo = notificationRepo;
    }

    // ─── Generate temp password ──────────────────────────────────
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#!";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++)
            sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    // ─── STATS ───────────────────────────────────────────────────

    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();
        stats.totalManagers  = userRepo.findByRole(Role.MANAGER).size();
        stats.totalEmployees = userRepo.findByRole(Role.EMPLOYEE).size();
        stats.totalLeaves    = leaveRepo.count();
        stats.pendingLeaves  = leaveRepo.findAll().stream()
                .filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
        stats.approvedLeaves = leaveRepo.findAll().stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        stats.rejectedLeaves = leaveRepo.findAll().stream()
                .filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();
        return stats;
    }

    // ─── USERS ───────────────────────────────────────────────────

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll()
                .stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getAllManagers() {
        return userRepo.findByRole(Role.MANAGER)
                .stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getAllEmployees() {
        return userRepo.findByRole(Role.EMPLOYEE)
                .stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User with ID " + id + " not found"));
        return UserResponse.from(user);
    }

    public UserResponse createManager(RegisterRequest request) {
        if (request.firstName == null || request.firstName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name is required");
        if (request.surname == null || request.surname.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Surname is required");
        if (request.emailAddress == null || request.emailAddress.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");

        if (userRepo.findByEmailAddress(request.emailAddress.trim().toLowerCase()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");

        String rawPassword = (request.password != null && request.password.length() >= 6)
                ? request.password
                : generateTempPassword();

        User user = new User();
        user.setFirstName(request.firstName.trim());
        user.setSurname(request.surname.trim());
        user.setEmailAddress(request.emailAddress.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setDepartment(request.department != null ? request.department.trim() : null);
        user.setRole(Role.MANAGER);
        userRepo.save(user);

        emailService.sendWelcomeEmail(
            user.getEmailAddress(),
            user.getFirstName(),
            "MANAGER",
            rawPassword
        );

        return UserResponse.from(user);
    }

    // ─── DELETE USER (fixed) ─────────────────────────────────────
    @Transactional
    public void deleteManager(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete an admin account");

        // 1. Delete this user's notifications
        notificationRepo.deleteByUserId(user.getId());

        // 2. Clear reviewer references (if manager who reviewed leaves is deleted)
        leaveRepo.clearReviewedBy(user.getId());

        // 3. If employee: delete their leave requests
        if (user.getRole() == Role.EMPLOYEE) {
            leaveRepo.deleteByEmployeeId(user.getId());
        }

        // 4. If manager: reassign their employees to no manager + delete their leaves
        if (user.getRole() == Role.MANAGER) {
            // Unassign employees from this manager
            List<User> subordinates = userRepo.findByManagerId(user.getId());
            subordinates.forEach(emp -> {
                emp.setManager(null);
                userRepo.save(emp);
            });

            // Delete all leaves belonging to employees of this manager
            subordinates.forEach(emp ->
                leaveRepo.deleteByEmployeeId(emp.getId())
            );

            // Delete delegation records
            delegationRepo.findByDelegatorIdAndActiveTrue(user.getId())
                .ifPresent(delegationRepo::delete);
            delegationRepo.findByDelegateIdAndActiveTrue(user.getId())
                .forEach(delegationRepo::delete);
        }

        // 5. Finally delete the user
        userRepo.delete(user);
    }

    // ─── LEAVES ──────────────────────────────────────────────────

    public List<LeaveResponse> getAllLeaves() {
        return leaveRepo.findAll()
                .stream().map(LeaveResponse::from).collect(Collectors.toList());
    }

    // ─── REPORTS ─────────────────────────────────────────────────

    public Object getSystemReports() {
        var allLeaves = leaveRepo.findAll();

        var byDepartment = allLeaves.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        l -> l.getEmployee().getDepartment() != null
                                ? l.getEmployee().getDepartment() : "Unknown",
                        java.util.stream.Collectors.counting()
                ));

        var byType = allLeaves.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        l -> l.getLeaveType(),
                        java.util.stream.Collectors.counting()
                ));

        var formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
        var byMonth = new java.util.LinkedHashMap<String, Long>();
        for (int i = 5; i >= 0; i--) {
            byMonth.put(java.time.LocalDate.now().minusMonths(i).format(formatter), 0L);
        }
        allLeaves.forEach(l -> {
            String key = l.getStartDate().format(formatter);
            if (byMonth.containsKey(key))
                byMonth.put(key, byMonth.get(key) + 1);
        });

        var managers = userRepo.findByRole(Role.MANAGER);
        var managerSummaries = managers.stream().map(m -> {
            var mLeaves = allLeaves.stream()
                    .filter(l -> l.getEmployee().getManager() != null
                            && l.getEmployee().getManager().getId().equals(m.getId()))
                    .collect(java.util.stream.Collectors.toList());
            return java.util.Map.of(
                "managerName",    m.getFirstName() + " " + m.getSurname(),
                "department",     m.getDepartment() != null ? m.getDepartment() : "N/A",
                "totalEmployees", userRepo.findByManagerId(m.getId()).size(),
                "totalLeaves",    mLeaves.size(),
                "pendingLeaves",  mLeaves.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count(),
                "approvedLeaves", mLeaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count(),
                "rejectedLeaves", mLeaves.stream().filter(l -> l.getStatus() == LeaveStatus.REJECTED).count()
            );
        }).collect(java.util.stream.Collectors.toList());

        return java.util.Map.of(
            "leavesByDepartment", byDepartment,
            "leavesByType",       byType,
            "leavesByMonth",      byMonth,
            "managerSummaries",   managerSummaries
        );
    }
}