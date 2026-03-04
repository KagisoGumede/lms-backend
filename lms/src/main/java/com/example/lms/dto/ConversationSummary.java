package com.example.lms.dto;

import java.time.LocalDateTime;

public class ConversationSummary {
    private Long contactId;
    private String contactName;
    private String contactRole;
    private String contactDepartment;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactRole() { return contactRole; }
    public void setContactRole(String contactRole) { this.contactRole = contactRole; }
    public String getContactDepartment() { return contactDepartment; }
    public void setContactDepartment(String contactDepartment) { this.contactDepartment = contactDepartment; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
}