package com.example.lms.service;

import com.example.lms.dto.RegisterRequest;
import com.example.lms.dto.UserResponse;
import com.example.lms.model.Role;
import com.example.lms.model.User;
import com.example.lms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailService;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       AuditLogService auditLogService,
                       EmailNotificationService emailService) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.emailService    = emailService;
    }

    // ─── Generate temp password ───────────────────────────────────
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#!";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++)
            sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    // ─── Create User ──────────────────────────────────────────────
    public UserResponse createUser(RegisterRequest request) {
        if (request.firstName == null || request.firstName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name is required");
        if (request.surname == null || request.surname.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Surname is required");
        if (request.emailAddress == null || request.emailAddress.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is required");
        if (request.role == null || request.role.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");

        if (userRepository.findByEmailAddress(request.emailAddress.trim().toLowerCase()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists");

        Role role;
        try {
            role = Role.valueOf(request.role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role '" + request.role + "'. Allowed values: EMPLOYEE, MANAGER");
        }

        User manager = null;
        if (request.managerId != null) {
            manager = userRepository.findById(request.managerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Manager with ID " + request.managerId + " not found"));
            if (manager.getRole() != Role.MANAGER)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User with ID " + request.managerId + " does not have the MANAGER role");
        }

        String rawPassword = (request.password != null && request.password.length() >= 6)
                ? request.password
                : generateTempPassword();

        User user = new User();
        user.setFirstName(request.firstName.trim());
        user.setSurname(request.surname.trim());
        user.setEmailAddress(request.emailAddress.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setDepartment(request.department != null ? request.department.trim() : null);
        user.setRole(role);
        user.setManager(manager);
        userRepository.save(user);

        String managerName = manager != null
            ? manager.getFirstName() + " " + manager.getSurname()
            : "No Manager";
        auditLogService.log(
            "USER_CREATED", managerName, "MANAGER",
            user.getFirstName() + " " + user.getSurname(),
            "Role: " + role.name()
                + (user.getDepartment() != null ? " | Dept: " + user.getDepartment() : "")
        );

        emailService.sendWelcomeEmail(
            user.getEmailAddress(), user.getFirstName(), role.name(), rawPassword
        );

        return UserResponse.from(user);
    }

    // ─── Get Users ────────────────────────────────────────────────
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Manager with ID " + managerId + " not found"));
        if (manager.getRole() != Role.MANAGER)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User with ID " + managerId + " is not a manager");
        return userRepository.findByManagerId(managerId)
                .stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User with ID " + userId + " not found"));
        return UserResponse.from(user);
    }

    public List<UserResponse> getAllManagers() {
        return userRepository.findByRole(Role.MANAGER)
                .stream().map(UserResponse::from).collect(Collectors.toList());
    }

    // ─── Update Profile ───────────────────────────────────────────
    public UserResponse updateProfile(Long userId, Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String firstName = body.get("firstName");
        String surname   = body.get("surname");
        String email     = body.get("emailAddress");

        if (firstName != null && !firstName.isBlank()) user.setFirstName(firstName.trim());
        if (surname   != null && !surname.isBlank())   user.setSurname(surname.trim());

        if (email != null && !email.isBlank()) {
            String newEmail = email.trim().toLowerCase();
            userRepository.findByEmailAddress(newEmail).ifPresent(existing -> {
                if (!existing.getId().equals(userId))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Email is already in use by another account");
            });
            user.setEmailAddress(newEmail);
        }

        userRepository.save(user);
        auditLogService.log("PROFILE_UPDATED",
            user.getFirstName() + " " + user.getSurname(),
            user.getRole().name(), null, "Profile updated");

        return UserResponse.from(user);
    }

    // ─── Change Password ──────────────────────────────────────────
    public Map<String, String> changePassword(Long userId, Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        if (currentPassword == null || currentPassword.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
        if (newPassword == null || newPassword.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must be at least 6 characters");
        if (!newPassword.equals(confirmPassword))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.log("PASSWORD_CHANGED",
            user.getFirstName() + " " + user.getSurname(),
            user.getRole().name(), null, "Password changed successfully");

        return Map.of("message", "Password changed successfully");
    }

    // ─── Admin Update User ────────────────────────────────────────
    public UserResponse updateUserByAdmin(Long userId, Map<String, Object> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot edit admin accounts");

        String department   = (String) body.get("department");
        String roleStr      = (String) body.get("role");
        Object managerIdObj = body.get("managerId");

        if (department != null && !department.isBlank())
            user.setDepartment(department.trim());

        if (roleStr != null && !roleStr.isBlank()) {
            try {
                Role newRole = Role.valueOf(roleStr.toUpperCase());
                if (newRole == Role.ADMIN)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign admin role");
                // If demoting from MANAGER to EMPLOYEE, unassign their team
                if (user.getRole() == Role.MANAGER && newRole == Role.EMPLOYEE) {
                    List<User> team = userRepository.findByManagerId(userId);
                    for (User member : team) {
                        member.setManager(null);
                        userRepository.save(member);
                    }
                }
                user.setRole(newRole);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
            }
        }

        if (body.containsKey("managerId")) {
            if (managerIdObj == null) {
                user.setManager(null);
            } else {
                Long managerId = Long.valueOf(managerIdObj.toString());
                User manager = userRepository.findById(managerId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Manager not found"));
                if (manager.getRole() != Role.MANAGER)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Selected user is not a manager");
                user.setManager(manager);
            }
        }

        userRepository.save(user);

        auditLogService.log("USER_UPDATED",
            "Admin", "ADMIN",
            user.getFirstName() + " " + user.getSurname(),
            "Role: " + user.getRole().name()
                + (user.getDepartment() != null ? " | Dept: " + user.getDepartment() : ""));

        return UserResponse.from(user);
    }

    // ─── Profile with Leave Summary ───────────────────────────────
    public Map<String, Object> getProfileWithSummary(Long userId,
            com.example.lms.repository.LeaveRequestRepository leaveRepo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var leaves = leaveRepo.findByEmployeeId(userId);
        long total    = leaves.size();
        long pending  = leaves.stream().filter(l -> l.getStatus().name().equals("PENDING")).count();
        long approved = leaves.stream().filter(l -> l.getStatus().name().equals("APPROVED")).count();
        long rejected = leaves.stream().filter(l -> l.getStatus().name().equals("REJECTED")).count();

        return Map.of(
            "id",           user.getId(),
            "firstName",    user.getFirstName(),
            "surname",      user.getSurname(),
            "emailAddress", user.getEmailAddress(),
            "department",   user.getDepartment() != null ? user.getDepartment() : "",
            "role",         user.getRole().name(),
            "managerName",  user.getManager() != null
                ? user.getManager().getFirstName() + " " + user.getManager().getSurname()
                : "N/A",
            "leaveSummary", Map.of(
                "total",    total,
                "pending",  pending,
                "approved", approved,
                "rejected", rejected
            )
        );
    }
}