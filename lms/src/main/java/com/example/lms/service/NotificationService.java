package com.example.lms.service;

import com.example.lms.model.Notification;
import com.example.lms.model.User;
import com.example.lms.repository.NotificationRepository;
import com.example.lms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final EmailNotificationService emailService;

    public NotificationService(NotificationRepository notificationRepo,
                                UserRepository userRepo,
                                EmailNotificationService emailService) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    public void send(Long userId, String title, String message, String type) {
        // Save in-app notification
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        notificationRepo.save(n);

        // Also send email
        userRepo.findById(userId).ifPresent(user ->
            emailService.sendNotificationEmail(
                user.getEmailAddress(),
                user.getFirstName(),
                title,
                message
            )
        );
    }

    public List<Notification> getForUser(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    public void markAllRead(Long userId) {
        notificationRepo.markAllReadByUserId(userId);
    }

    public void markOneRead(Long notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }
}