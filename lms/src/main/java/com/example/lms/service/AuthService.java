package com.example.lms.service;

import com.example.lms.dto.LoginRequest;
import com.example.lms.dto.LoginResponse;
import com.example.lms.model.User;
import com.example.lms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {

        if (request.emailAddress == null || request.emailAddress.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");

        if (request.password == null || request.password.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");

        User user = userRepository.findByEmailAddress(request.emailAddress.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        if (!passwordEncoder.matches(request.password, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");

        return new LoginResponse(
                user.getId(),
                user.getFirstName(),
                user.getSurname(),
                user.getEmailAddress(),
                user.getDepartment(),
                user.getRole().name()
        );
    }
}