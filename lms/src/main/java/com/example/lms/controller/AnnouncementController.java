package com.example.lms.controller;

import com.example.lms.dto.AnnouncementRequest;
import com.example.lms.dto.AnnouncementResponse;
import com.example.lms.dto.ApiResponse;
import com.example.lms.service.AnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(
            @RequestBody AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Announcement created successfully", announcementService.create(request)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getForUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
            ApiResponse.ok("Announcements fetched", announcementService.getForUser(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getAll() {
        return ResponseEntity.ok(
            ApiResponse.ok("All announcements fetched", announcementService.getAll()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getActive() {
        return ResponseEntity.ok(
            ApiResponse.ok("Active announcements fetched", announcementService.getActive()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> update(
            @PathVariable Long id, @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(
            ApiResponse.ok("Announcement updated", announcementService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivate(@PathVariable Long id) {
        announcementService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Announcement deactivated", "OK"));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> togglePin(@PathVariable Long id) {
        return ResponseEntity.ok(
            ApiResponse.ok("Pin status updated", announcementService.togglePin(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Announcement deleted", "OK"));
    }
}
