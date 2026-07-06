package com.devspec.service;

import com.devspec.model.*;
import com.devspec.repository.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReportRepository reportRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final NotificationRepository notificationRepository;

    @Value("${devspec.upload.dir}")
    private String uploadBaseDir;

    public BackupService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ReportRepository reportRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            SystemSettingRepository systemSettingRepository,
            AuditLogRepository auditLogRepository,
            AiUsageLogRepository aiUsageLogRepository,
            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiUsageLogRepository = aiUsageLogRepository;
        this.notificationRepository = notificationRepository;
    }

    public byte[] createBackupArchive() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        // 1. Serialize and write database tables as JSON
        addDatabaseJsonToZip(zos);

        // 2. Archive uploaded project folders/zips
        addUploadedFilesToZip(zos);

        zos.close();
        return baos.toByteArray();
    }

    private void addDatabaseJsonToZip(ZipOutputStream zos) throws IOException {
        writeJsonEntry(zos, "database/users.json", serializeUsers());
        writeJsonEntry(zos, "database/projects.json", serializeProjects());
        writeJsonEntry(zos, "database/reports.json", serializeReports());
        writeJsonEntry(zos, "database/review_history.json", serializeReviewHistories());
        writeJsonEntry(zos, "database/system_settings.json", serializeSystemSettings());
        writeJsonEntry(zos, "database/audit_logs.json", serializeAuditLogs());
        writeJsonEntry(zos, "database/ai_usage_logs.json", serializeAiUsageLogs());
        writeJsonEntry(zos, "database/notifications.json", serializeNotifications());
    }

    private void writeJsonEntry(ZipOutputStream zos, String fileName, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(fileName));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String serializeUsers() {
        JSONArray arr = new JSONArray();
        for (User u : userRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", u.getId());
            obj.put("username", u.getUsername());
            obj.put("email", u.getEmail());
            obj.put("role", u.getRole());
            obj.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeProjects() {
        JSONArray arr = new JSONArray();
        for (Project p : projectRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", p.getId());
            obj.put("name", p.getName());
            obj.put("type", p.getType());
            obj.put("repoUrl", p.getRepoUrl());
            obj.put("filePath", p.getFilePath());
            obj.put("userId", p.getUser() != null ? p.getUser().getId() : null);
            obj.put("tags", new JSONArray(p.getTags()));
            obj.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeReports() {
        JSONArray arr = new JSONArray();
        for (Report r : reportRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", r.getId());
            obj.put("reportVersion", r.getReportVersion());
            obj.put("projectVersion", r.getProjectVersion());
            obj.put("overallScore", r.getOverallScore());
            obj.put("architectureScore", r.getArchitectureScore());
            obj.put("codeQualityScore", r.getCodeQualityScore());
            obj.put("securityScore", r.getSecurityScore());
            obj.put("testingScore", r.getTestingScore());
            obj.put("maintainabilityScore", r.getMaintainabilityScore());
            obj.put("documentationScore", r.getDocumentationScore());
            obj.put("performanceScore", r.getPerformanceScore());
            obj.put("projectId", r.getProject() != null ? r.getProject().getId() : null);
            obj.put("reviewer", r.getReviewer());
            obj.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeReviewHistories() {
        JSONArray arr = new JSONArray();
        for (ReviewHistory rh : reviewHistoryRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", rh.getId());
            obj.put("analysisId", rh.getAnalysisId());
            obj.put("status", rh.getStatus());
            obj.put("stage", rh.getStage());
            obj.put("projectId", rh.getProject() != null ? rh.getProject().getId() : null);
            obj.put("createdAt", rh.getCreatedAt() != null ? rh.getCreatedAt().toString() : "");
            obj.put("completedAt", rh.getCompletedAt() != null ? rh.getCompletedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeSystemSettings() {
        JSONArray arr = new JSONArray();
        for (SystemSetting ss : systemSettingRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("keyName", ss.getKeyName());
            obj.put("valueContent", ss.getValueContent());
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeAuditLogs() {
        JSONArray arr = new JSONArray();
        for (AuditLog al : auditLogRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", al.getId());
            obj.put("username", al.getUsername());
            obj.put("operation", al.getOperation());
            obj.put("status", al.getStatus());
            obj.put("ipAddress", al.getIpAddress());
            obj.put("details", al.getDetails());
            obj.put("createdAt", al.getCreatedAt() != null ? al.getCreatedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeAiUsageLogs() {
        JSONArray arr = new JSONArray();
        for (AiUsageLog au : aiUsageLogRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", au.getId());
            obj.put("provider", au.getProvider());
            obj.put("tokensUsed", au.getTokensUsed());
            obj.put("requestTimeMs", au.getRequestTimeMs());
            obj.put("costEstimate", au.getCostEstimate());
            obj.put("status", au.getStatus());
            obj.put("username", au.getUsername());
            obj.put("operation", au.getOperation());
            obj.put("createdAt", au.getCreatedAt() != null ? au.getCreatedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private String serializeNotifications() {
        JSONArray arr = new JSONArray();
        for (Notification n : notificationRepository.findAll()) {
            JSONObject obj = new JSONObject();
            obj.put("id", n.getId());
            obj.put("username", n.getUsername());
            obj.put("type", n.getType());
            obj.put("message", n.getMessage());
            obj.put("isRead", n.getIsRead());
            obj.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
            arr.put(obj);
        }
        return arr.toString(2);
    }

    private void addUploadedFilesToZip(ZipOutputStream zos) throws IOException {
        File uploadDir = new File(uploadBaseDir);
        if (!uploadDir.exists() || !uploadDir.isDirectory()) {
            return;
        }
        zipDirectory(uploadDir, uploadDir.getName(), zos);
    }

    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] bytesIn = new byte[4096];
                int read = 0;
                while ((read = bis.read(bytesIn)) != -1) {
                    zos.write(bytesIn, 0, read);
                }
            }
            zos.closeEntry();
        }
    }
}
