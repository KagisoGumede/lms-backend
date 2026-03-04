package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.dto.LoginRequest;
import com.example.lms.dto.LoginResponse;
import com.example.lms.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     * {
     *   "emailAddress": "john@example.com",
     *   "password": "secret123"
     * }
     *
     * Returns user info + role. Frontend uses `role` to redirect:
     *   MANAGER  -> manager dashboard
     *   EMPLOYEE -> employee dashboard
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", loginResponse));
    }
}
