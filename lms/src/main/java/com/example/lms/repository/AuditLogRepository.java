package com.example.lms.repository;



import com.example.lms.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<AuditLog> findByPerformedByContainingIgnoreCaseOrderByTimestampDesc(String name);
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
}