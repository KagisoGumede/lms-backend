package com.example.lms.service;

import com.example.lms.model.User;
import com.example.lms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                JavaMailSender mailSender,
                                BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    // Step 1 — Generate token and send email
    public void requestReset(String emailAddress) {
        User user = userRepository.findByEmailAddress(emailAddress.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No account found with this email address"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmailAddress());
        message.setSubject("LMS - Password Reset Request");
        message.setText(
            "Hi " + user.getFirstName() + ",\n\n" +
            "You requested a password reset. Click the link below:\n\n" +
            resetLink + "\n\n" +
            "This link expires in 30 minutes.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "LMS Team"
        );
        mailSender.send(message);
    }

    // Step 2 — Validate token and update password
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid or expired reset token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reset token has expired. Please request a new one.");

        if (newPassword == null || newPassword.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 6 characters");

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}