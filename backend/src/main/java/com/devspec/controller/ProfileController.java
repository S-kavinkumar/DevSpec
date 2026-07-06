package com.devspec.controller;

import com.devspec.model.AiUsageLog;
import com.devspec.model.Project;
import com.devspec.model.Report;
import com.devspec.model.User;
import com.devspec.repository.*;
import com.devspec.service.AuditLogService;
import com.devspec.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReportRepository reportRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ProfileController(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ReportRepository reportRepository,
            AiUsageLogRepository aiUsageLogRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.aiUsageLogRepository = aiUsageLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @GetMapping
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Project> projects = projectRepository.findByUserId(user.getId());
        
        long reportCount = 0;
        double scoreSum = 0;
        List<Report> userReports = new ArrayList<>();
        for (Project p : projects) {
            List<Report> reps = reportRepository.findByProjectIdOrderByCreatedAtDesc(p.getId());
            userReports.addAll(reps);
            reportCount += reps.size();
            for (Report r : reps) {
                scoreSum += r.getOverallScore();
            }
        }
        double avgScore = reportCount > 0 ? (scoreSum / reportCount) : 0.0;

        List<AiUsageLog> aiLogs = aiUsageLogRepository.findByUsernameOrderByCreatedAtDesc(user.getUsername());
        double aiCost = 0.0;
        for (AiUsageLog log : aiLogs) {
            aiCost += log.getCostEstimate();
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", user.getId());
        profileData.put("username", user.getUsername());
        profileData.put("email", user.getEmail());
        profileData.put("role", user.getRole());
        profileData.put("createdAt", user.getCreatedAt());
        profileData.put("totalProjectsUploaded", projects.size());
        profileData.put("totalReportsGenerated", reportCount);
        profileData.put("averageProjectScore", Math.round(avgScore * 10.0) / 10.0);
        profileData.put("totalAiCost", Math.round(aiCost * 100.0) / 100.0);
        profileData.put("aiLogs", aiLogs);

        return ResponseEntity.ok(profileData);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String email = payload.get("email");
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email.trim());
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                return ResponseEntity.badRequest().body("Current password is required to change to a new password");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Current password does not match");
            }
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        userRepository.save(user);
        
        auditLogService.log(user.getUsername(), "Update Profile", "SUCCESS", getClientIp(request),
                "Updated user account profile information.");

        notificationService.notify(user.getUsername(), "SUCCESS", "Your user profile credentials have been updated successfully.");

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }
}
