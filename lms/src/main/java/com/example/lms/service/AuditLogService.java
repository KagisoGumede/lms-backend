package com.example.lms.service;



import com.example.lms.model.AuditLog;
import com.example.lms.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String action, String performedBy, String performedByRole,
                    String targetUser, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setPerformedByRole(performedByRole);
        log.setTargetUser(targetUser);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public Page<AuditLog> getAllLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    public List<AuditLog> getLogsByUser(String name) {
        return auditLogRepository.findByPerformedByContainingIgnoreCaseOrderByTimestampDesc(name);
    }

    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action);
    }
}