package com.example.lms.service;

import com.example.lms.dto.AnnouncementRequest;
import com.example.lms.dto.AnnouncementResponse;
import com.example.lms.model.Announcement;
import com.example.lms.model.Role;
import com.example.lms.model.User;
import com.example.lms.repository.AnnouncementRepository;
import com.example.lms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final TeamsNotificationService teamsService;

    public AnnouncementService(AnnouncementRepository announcementRepo,
                                UserRepository userRepo,
                                NotificationService notificationService,
                                TeamsNotificationService teamsService) {
        this.announcementRepo = announcementRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.teamsService = teamsService;
    }

    public AnnouncementResponse create(AnnouncementRequest req) {
        Announcement a = new Announcement();
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        a.setCategory(req.getCategory() != null ? req.getCategory() : "GENERAL");
        a.setTargetAudience(req.getTargetAudience() != null ? req.getTargetAudience() : "ALL");
        a.setTargetDepartment(req.getTargetDepartment());
        a.setTargetRole(req.getTargetRole());
        a.setPinned(req.isPinned());
        a.setExpiresAt(req.getExpiresAt());
        a.setCreatedByUserId(req.getCreatedByUserId());

        userRepo.findById(req.getCreatedByUserId()).ifPresent(user ->
            a.setCreatedByName(user.getFirstName() + " " + user.getSurname())
        );

        Announcement saved = announcementRepo.save(a);
        notifyTargetedUsers(saved);

        // ─── Notify Teams ─────────────────────────────────────────
        teamsService.notifyAnnouncement(
            saved.getTitle(),
            saved.getContent(),
            saved.getCategory(),
            saved.getCreatedByName() != null ? saved.getCreatedByName() : "Admin"
        );

        return AnnouncementResponse.from(saved);
    }

    public List<AnnouncementResponse> getForUser(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return announcementRepo.findVisibleForUser(
                LocalDateTime.now(),
                user.getDepartment(),
                user.getRole().name()
            )
            .stream()
            .map(AnnouncementResponse::from)
            .collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getAll() {
        return announcementRepo.findAllByOrderByPinnedDescCreatedAtDesc()
            .stream()
            .map(AnnouncementResponse::from)
            .collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getActive() {
        return announcementRepo.findAllActive(LocalDateTime.now())
            .stream()
            .map(AnnouncementResponse::from)
            .collect(Collectors.toList());
    }

    public AnnouncementResponse update(Long id, AnnouncementRequest req) {
        Announcement a = announcementRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));

        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getContent() != null) a.setContent(req.getContent());
        if (req.getCategory() != null) a.setCategory(req.getCategory());
        if (req.getTargetAudience() != null) a.setTargetAudience(req.getTargetAudience());
        if (req.getTargetDepartment() != null) a.setTargetDepartment(req.getTargetDepartment());
        if (req.getTargetRole() != null) a.setTargetRole(req.getTargetRole());
        if (req.getExpiresAt() != null) a.setExpiresAt(req.getExpiresAt());
        a.setPinned(req.isPinned());

        return AnnouncementResponse.from(announcementRepo.save(a));
    }

    public void deactivate(Long id) {
        Announcement a = announcementRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        a.setActive(false);
        announcementRepo.save(a);
    }

    public void delete(Long id) {
        announcementRepo.deleteById(id);
    }

    public AnnouncementResponse togglePin(Long id) {
        Announcement a = announcementRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found: " + id));
        a.setPinned(!a.isPinned());
        return AnnouncementResponse.from(announcementRepo.save(a));
    }

    private void notifyTargetedUsers(Announcement a) {
        List<User> targets;

        if ("DEPARTMENT".equals(a.getTargetAudience())) {
            targets = userRepo.findAll().stream()
                .filter(u -> a.getTargetDepartment().equals(u.getDepartment()))
                .collect(Collectors.toList());
        } else if ("ROLE".equals(a.getTargetAudience())) {
            Role role = Role.valueOf(a.getTargetRole());
            targets = userRepo.findByRole(role);
        } else {
            targets = userRepo.findAll();
        }

        for (User user : targets) {
            String preview = a.getContent().length() > 100
                ? a.getContent().substring(0, 100) + "..."
                : a.getContent();
            notificationService.send(
                user.getId(),
                "New Announcement: " + a.getTitle(),
                preview,
                "ANNOUNCEMENT"
            );
        }
    }
}