package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.service.TeamsNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin
public class TeamsController {

    private final TeamsNotificationService teamsService;

    @Value("${teams.notifications.enabled:false}")
    private boolean enabled;

    @Value("${teams.webhook.url:}")
    private String webhookUrl;

    public TeamsController(TeamsNotificationService teamsService) {
        this.teamsService = teamsService;
    }

    // Get current Teams config status
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        boolean configured = webhookUrl != null
            && !webhookUrl.isBlank()
            && !webhookUrl.contains("YOUR_WEBHOOK_URL_HERE");
        return ResponseEntity.ok(ApiResponse.ok("Teams status fetched", Map.of(
            "enabled", enabled,
            "configured", configured,
            "webhookConfigured", configured
        )));
    }

    // Send a test notification to Teams
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> sendTest() {
        teamsService.notifyAnnouncement(
            "Teams Integration Test",
            "Your Microsoft Teams integration is working correctly! " +
            "You will now receive notifications for leave requests, approvals, announcements and messages.",
            "GENERAL",
            "System Administrator"
        );
        return ResponseEntity.ok(ApiResponse.ok("Test notification sent to Teams", "OK"));
    }
}