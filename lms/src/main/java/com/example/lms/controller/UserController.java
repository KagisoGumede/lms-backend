package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.dto.RegisterRequest;
import com.example.lms.dto.UserResponse;
import com.example.lms.repository.LeaveRequestRepository;
import com.example.lms.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService userService;
    private final LeaveRequestRepository leaveRepo;

    public UserController(UserService userService, LeaveRequestRepository leaveRepo) {
        this.userService = userService;
        this.leaveRepo   = leaveRepo;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody RegisterRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User fetched", userService.getUserById(id)));
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(ApiResponse.ok("Team fetched", userService.getUsersByManager(managerId)));
    }

    @GetMapping("/managers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllManagers() {
        return ResponseEntity.ok(ApiResponse.ok("Managers fetched", userService.getAllManagers()));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched",
                userService.getProfileWithSummary(id, leaveRepo)));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully",
                userService.updateProfile(id, body)));
    }

    // ─── Change Password ─────────────────────────────────────────
    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully",
                userService.changePassword(id, body)));
    }

    // ─── Admin Update User ────────────────────────────────────────
    @PutMapping("/{id}/admin-update")
    public ResponseEntity<ApiResponse<UserResponse>> adminUpdateUser(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully",
                userService.updateUserByAdmin(id, body)));
    }
}