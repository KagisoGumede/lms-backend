package com.example.lms.repository;

import com.example.lms.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Get full conversation between two users
    @Query("""
        SELECT m FROM Message m
        WHERE (m.senderId = :userA AND m.receiverId = :userB)
           OR (m.senderId = :userB AND m.receiverId = :userA)
        ORDER BY m.sentAt ASC
    """)
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    // Get all unique people this user has chatted with
    @Query("""
        SELECT DISTINCT CASE
            WHEN m.senderId = :userId THEN m.receiverId
            ELSE m.senderId
        END
        FROM Message m
        WHERE m.senderId = :userId OR m.receiverId = :userId
    """)
    List<Long> findContactIds(@Param("userId") Long userId);

    // Count unread messages for a user
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    // Count unread from a specific sender
    long countBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);

    // Mark all messages in a conversation as read
    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.isRead = false
    """)
    void markConversationRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    // Latest message between two users
    @Query("""
        SELECT m FROM Message m
        WHERE (m.senderId = :userA AND m.receiverId = :userB)
           OR (m.senderId = :userB AND m.receiverId = :userA)
        ORDER BY m.sentAt DESC
    """)
    List<Message> findLatestBetween(@Param("userA") Long userA, @Param("userB") Long userB);
}
