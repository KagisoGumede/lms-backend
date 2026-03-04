package com.example.lms.service;

import com.example.lms.dto.ConversationSummary;
import com.example.lms.dto.MessageRequest;
import com.example.lms.dto.MessageResponse;
import com.example.lms.model.Message;
import com.example.lms.model.User;
import com.example.lms.repository.MessageRepository;
import com.example.lms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final TeamsNotificationService teamsService;

    public MessageService(MessageRepository messageRepo,
                          UserRepository userRepo,
                          TeamsNotificationService teamsService) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.teamsService = teamsService;
    }

    // ─── Send a message ───────────────────────────────────────────
    public MessageResponse send(MessageRequest req) {
        User sender = userRepo.findById(req.getSenderId())
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepo.findById(req.getReceiverId())
            .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message m = new Message();
        m.setSenderId(req.getSenderId());
        m.setSenderName(sender.getFirstName() + " " + sender.getSurname());
        m.setReceiverId(req.getReceiverId());
        m.setReceiverName(receiver.getFirstName() + " " + receiver.getSurname());
        m.setContent(req.getContent());

        MessageResponse response = MessageResponse.from(messageRepo.save(m));

        // ─── Notify Teams ─────────────────────────────────────────
        String preview = req.getContent().length() > 80
            ? req.getContent().substring(0, 80) + "..."
            : req.getContent();
        teamsService.notifyNewMessage(
            m.getSenderName(),
            m.getReceiverName(),
            preview
        );

        return response;
    }

    // ─── Get full conversation between two users ──────────────────
    public List<MessageResponse> getConversation(Long userA, Long userB) {
        messageRepo.markConversationRead(userB, userA);
        return messageRepo.findConversation(userA, userB)
            .stream().map(MessageResponse::from).collect(Collectors.toList());
    }

    // ─── Get inbox: list of all conversations for a user ─────────
    public List<ConversationSummary> getInbox(Long userId) {
        List<Long> contactIds = messageRepo.findContactIds(userId);
        List<ConversationSummary> inbox = new ArrayList<>();

        for (Long contactId : contactIds) {
            userRepo.findById(contactId).ifPresent(contact -> {
                ConversationSummary cs = new ConversationSummary();
                cs.setContactId(contactId);
                cs.setContactName(contact.getFirstName() + " " + contact.getSurname());
                cs.setContactRole(contact.getRole().name());
                cs.setContactDepartment(contact.getDepartment());

                List<Message> latest = messageRepo.findLatestBetween(userId, contactId);
                if (!latest.isEmpty()) {
                    Message last = latest.get(0);
                    String preview = last.getContent().length() > 60
                        ? last.getContent().substring(0, 60) + "..."
                        : last.getContent();
                    cs.setLastMessage(preview);
                    cs.setLastMessageTime(last.getSentAt());
                }

                cs.setUnreadCount(messageRepo.countBySenderIdAndReceiverIdAndIsReadFalse(contactId, userId));
                inbox.add(cs);
            });
        }

        inbox.sort((a, b) -> {
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return inbox;
    }

    // ─── Total unread count for a user ───────────────────────────
    public long getUnreadCount(Long userId) {
        return messageRepo.countByReceiverIdAndIsReadFalse(userId);
    }

    // ─── Get all users except self (for new conversation) ────────
    public List<User> getAllUsersExcept(Long userId) {
        return userRepo.findAll().stream()
            .filter(u -> !u.getId().equals(userId))
            .collect(Collectors.toList());
    }
}