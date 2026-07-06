package com.devspec.service;

import com.devspec.model.AuditLog;
import com.devspec.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String operation, String status, String details) {
        log(username, operation, status, "0.0.0.0", details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String operation, String status, String ipAddress, String details) {
        AuditLog auditLog = AuditLog.builder()
                .username(username != null ? username : "anonymous")
                .operation(operation)
                .status(status)
                .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }
}
