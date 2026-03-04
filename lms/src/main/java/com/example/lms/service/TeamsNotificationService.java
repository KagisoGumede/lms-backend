package com.example.lms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TeamsNotificationService {

    @Value("${teams.webhook.url:}")
    private String webhookUrl;

    @Value("${teams.notifications.enabled:false}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Generic send ──────────────────────────────────────────────
    private void send(String payload) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()
                || webhookUrl.contains("YOUR_WEBHOOK_URL_HERE")) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Teams notification failed: " + e.getMessage());
        }
    }

    // ── Build adaptive card payload ───────────────────────────────
    private String buildCard(String color, String title, String subtitle, String body) {
        return """
            {
              "@type": "MessageCard",
              "@context": "http://schema.org/extensions",
              "themeColor": "%s",
              "summary": "%s",
              "sections": [{
                "activityTitle": "%s",
                "activitySubtitle": "%s",
                "activityText": "%s",
                "markdown": true
              }]
            }
            """.formatted(color, title, title, subtitle, body);
    }

    // ── Leave Request Submitted ───────────────────────────────────
    public void notifyLeaveSubmitted(String employeeName, String leaveType,
                                      String startDate, String endDate,
                                      String managerName) {
        String payload = buildCard(
            "F59E0B",
            "New Leave Request Submitted",
            "Submitted by **" + employeeName + "**",
            "**Leave Type:** " + leaveType + "\\n\\n" +
            "**Period:** " + startDate + " to " + endDate + "\\n\\n" +
            "**Assigned to:** " + managerName + " for review"
        );
        send(payload);
    }

    // ── Leave Approved ────────────────────────────────────────────
    public void notifyLeaveApproved(String employeeName, String leaveType,
                                     String startDate, String endDate,
                                     String managerName) {
        String payload = buildCard(
            "10B981",
            "Leave Request Approved ✅",
            "**" + employeeName + "'s** leave has been approved",
            "**Leave Type:** " + leaveType + "\\n\\n" +
            "**Period:** " + startDate + " to " + endDate + "\\n\\n" +
            "**Approved by:** " + managerName
        );
        send(payload);
    }

    // ── Leave Rejected ────────────────────────────────────────────
    public void notifyLeaveRejected(String employeeName, String leaveType,
                                     String startDate, String endDate,
                                     String managerName) {
        String payload = buildCard(
            "EF4444",
            "Leave Request Rejected ❌",
            "**" + employeeName + "'s** leave has been rejected",
            "**Leave Type:** " + leaveType + "\\n\\n" +
            "**Period:** " + startDate + " to " + endDate + "\\n\\n" +
            "**Rejected by:** " + managerName
        );
        send(payload);
    }

    // ── New Announcement ──────────────────────────────────────────
    public void notifyAnnouncement(String title, String content,
                                    String category, String createdBy) {
        String color = switch (category) {
            case "URGENT"  -> "EF4444";
            case "HOLIDAY" -> "10B981";
            case "POLICY"  -> "F59E0B";
            default        -> "3B82F6";
        };
        String preview = content.length() > 150
            ? content.substring(0, 150) + "..."
            : content;
        String payload = buildCard(
            color,
            "📢 New Announcement: " + title,
            "Posted by **" + createdBy + "**  |  Category: **" + category + "**",
            preview
        );
        send(payload);
    }

    // ── New Message ───────────────────────────────────────────────
    public void notifyNewMessage(String senderName, String receiverName,
                                  String preview) {
        String payload = buildCard(
            "6366F1",
            "💬 New Internal Message",
            "**" + senderName + "** sent a message to **" + receiverName + "**",
            "**Preview:** " + preview
        );
        send(payload);
    }
}