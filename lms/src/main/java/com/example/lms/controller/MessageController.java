package com.example.lms.controller;

import com.example.lms.dto.ApiResponse;
import com.example.lms.dto.ConversationSummary;
import com.example.lms.dto.MessageRequest;
import com.example.lms.dto.MessageResponse;
import com.example.lms.dto.UserResponse;
import com.example.lms.model.User;
import com.example.lms.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Send a message
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> send(@RequestBody MessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Message sent", messageService.send(request)));
    }

    // Get conversation between two users
    @GetMapping("/conversation/{userA}/{userB}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getConversation(
            @PathVariable Long userA, @PathVariable Long userB) {
        return ResponseEntity.ok(ApiResponse.ok("Conversation fetched",
            messageService.getConversation(userA, userB)));
    }

    // Get inbox (all conversations) for a user
    @GetMapping("/inbox/{userId}")
    public ResponseEntity<ApiResponse<List<ConversationSummary>>> getInbox(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Inbox fetched", messageService.getInbox(userId)));
    }

    // Get unread message count
    @GetMapping("/unread/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Unread count",
            Map.of("count", messageService.getUnreadCount(userId))));
    }

    // Get all users to start a new conversation with
    @GetMapping("/contacts/{userId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getContacts(@PathVariable Long userId) {
        List<UserResponse> contacts = messageService.getAllUsersExcept(userId)
            .stream().map(UserResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Contacts fetched", contacts));
    }
}