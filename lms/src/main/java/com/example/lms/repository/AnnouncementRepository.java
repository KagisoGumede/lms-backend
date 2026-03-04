package com.example.lms.repository;

import com.example.lms.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("""
        SELECT a FROM Announcement a
        WHERE a.active = true
        AND (a.expiresAt IS NULL OR a.expiresAt > :now)
        AND (
            a.targetAudience = 'ALL'
            OR (a.targetAudience = 'DEPARTMENT' AND a.targetDepartment = :department)
            OR (a.targetAudience = 'ROLE' AND a.targetRole = :role)
        )
        ORDER BY a.pinned DESC, a.createdAt DESC
    """)
    List<Announcement> findVisibleForUser(
        @Param("now") LocalDateTime now,
        @Param("department") String department,
        @Param("role") String role
    );

    List<Announcement> findAllByOrderByPinnedDescCreatedAtDesc();

    @Query("""
        SELECT a FROM Announcement a
        WHERE a.active = true
        AND (a.expiresAt IS NULL OR a.expiresAt > :now)
        ORDER BY a.pinned DESC, a.createdAt DESC
    """)
    List<Announcement> findAllActive(@Param("now") LocalDateTime now);
}