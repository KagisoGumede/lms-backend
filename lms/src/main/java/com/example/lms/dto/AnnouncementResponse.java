package com.example.lms.dto;

import com.example.lms.model.Announcement;
import java.time.LocalDateTime;

public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String targetAudience;
    private String targetDepartment;
    private String targetRole;
    private boolean pinned;
    private boolean active;
    private Long createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static AnnouncementResponse from(Announcement a) {
        AnnouncementResponse r = new AnnouncementResponse();
        r.id = a.getId();
        r.title = a.getTitle();
        r.content = a.getContent();
        r.category = a.getCategory();
        r.targetAudience = a.getTargetAudience();
        r.targetDepartment = a.getTargetDepartment();
        r.targetRole = a.getTargetRole();
        r.pinned = a.isPinned();
        r.active = a.isActive();
        r.createdByUserId = a.getCreatedByUserId();
        r.createdByName = a.getCreatedByName();
        r.createdAt = a.getCreatedAt();
        r.expiresAt = a.getExpiresAt();
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getTargetAudience() { return targetAudience; }
    public String getTargetDepartment() { return targetDepartment; }
    public String getTargetRole() { return targetRole; }
    public boolean isPinned() { return pinned; }
    public boolean isActive() { return active; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public String getCreatedByName() { return createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}