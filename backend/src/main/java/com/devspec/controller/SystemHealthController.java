package com.devspec.controller;

import com.devspec.model.AuditLog;
import com.devspec.repository.AuditLogRepository;
import com.devspec.service.SystemHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    private final SystemHealthService systemHealthService;
    private final AuditLogRepository auditLogRepository;

    public SystemHealthController(
            SystemHealthService systemHealthService,
            AuditLogRepository auditLogRepository) {
        this.systemHealthService = systemHealthService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(systemHealthService.getSystemHealth());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findTop100ByOrderByCreatedAtDesc());
    }
}
