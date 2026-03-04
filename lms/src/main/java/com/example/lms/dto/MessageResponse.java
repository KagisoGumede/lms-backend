package com.example.lms.dto;

import com.example.lms.model.Message;
import java.time.LocalDateTime;

public class MessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private boolean read;
    private LocalDateTime sentAt;

    public static MessageResponse from(Message m) {
        MessageResponse r = new MessageResponse();
        r.id = m.getId();
        r.senderId = m.getSenderId();
        r.senderName = m.getSenderName();
        r.receiverId = m.getReceiverId();
        r.receiverName = m.getReceiverName();
        r.content = m.getContent();
        r.read = m.isRead();
        r.sentAt = m.getSentAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public Long getReceiverId() { return receiverId; }
    public String getReceiverName() { return receiverName; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public LocalDateTime getSentAt() { return sentAt; }
}