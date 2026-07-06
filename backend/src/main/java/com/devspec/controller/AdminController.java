package com.devspec.controller;

import com.devspec.model.*;
import com.devspec.repository.*;
import com.devspec.service.AuditLogService;
import com.devspec.service.BackupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReportRepository reportRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final NotificationRepository notificationRepository;
    private final BackupService backupService;
    private final AuditLogService auditLogService;

    @Value("${devspec.upload.dir}")
    private String uploadBaseDir;

    @Value("${ai.provider:gemini}")
    private String activeProvider;

    public AdminController(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ReportRepository reportRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            AuditLogRepository auditLogRepository,
            AiUsageLogRepository aiUsageLogRepository,
            NotificationRepository notificationRepository,
            BackupService backupService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiUsageLogRepository = aiUsageLogRepository;
        this.notificationRepository = notificationRepository;
        this.backupService = backupService;
        this.auditLogService = auditLogService;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // --- Users Management ---

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Prevent editing self
        if (user.getUsername().equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.badRequest().body("You cannot modify your own administrative privileges");
        }

        String newRole = payload.get("role");
        if (!"ADMIN".equalsIgnoreCase(newRole) && !"USER".equalsIgnoreCase(newRole)) {
            return ResponseEntity.badRequest().body("Invalid role parameter");
        }

        user.setRole(newRole.toUpperCase());
        userRepository.save(user);

        auditLogService.log(authentication.getName(), "Update User Role", "SUCCESS", getClientIp(request),
                "Changed role of user '" + user.getUsername() + "' to " + newRole);

        return ResponseEntity.ok(Map.of("message", "User role updated to " + newRole));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getUsername().equalsIgnoreCase(authentication.getName())) {
            return ResponseEntity.badRequest().body("You cannot delete your own administrative account");
        }

        userRepository.delete(user);
        auditLogService.log(authentication.getName(), "Delete User", "SUCCESS", getClientIp(request),
                "Deleted user account: " + user.getUsername());

        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // --- Project/Report management ---

    @GetMapping("/projects")
    public ResponseEntity<?> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        projectRepository.delete(project);
        auditLogService.log(authentication.getName(), "Delete Project (Admin)", "SUCCESS", getClientIp(request),
                "Deleted project '" + project.getName() + "' and related audit assets.");

        return ResponseEntity.ok(Map.of("message", "Project and associated reports deleted successfully"));
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports() {
        return ResponseEntity.ok(reportRepository.findAll());
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id, Authentication authentication, HttpServletRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        reportRepository.delete(report);
        auditLogService.log(authentication.getName(), "Delete Report (Admin)", "SUCCESS", getClientIp(request),
                "Deleted report version " + report.getReportVersion() + " of project " + report.getProject().getName());

        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }

    // --- Active & Failed Pipeline runs ---

    @GetMapping("/failures")
    public ResponseEntity<?> getFailedRuns() {
        return ResponseEntity.ok(reviewHistoryRepository.findByStatus("FAILED"));
    }

    @GetMapping("/active-jobs")
    public ResponseEntity<?> getActiveJobs() {
        List<ReviewHistory> activeJobs = new ArrayList<>();
        List<String> activeStatuses = Arrays.asList("QUEUED", "VALIDATING", "EXTRACTING", "ANALYZING", "STATIC_ANALYSIS", "RUNNING_TESTS", "GENERATING_AI_REVIEW", "GENERATING_REPORT");
        for (ReviewHistory rh : reviewHistoryRepository.findAll()) {
            if (activeStatuses.contains(rh.getStatus())) {
                activeJobs.add(rh);
            }
        }
        return ResponseEntity.ok(activeJobs);
    }

    // --- System Analytics ---

    @GetMapping("/analytics")
    public ResponseEntity<?> getSystemAnalytics() {
        long totalUsers = userRepository.count();
        long totalProjects = projectRepository.count();
        long totalReports = reportRepository.count();
        long totalFailures = reviewHistoryRepository.findByStatus("FAILED").size();
        long totalCompleted = reviewHistoryRepository.findByStatus("COMPLETED").size();

        double successRate = (totalCompleted + totalFailures > 0)
                ? (totalCompleted * 100.0 / (totalCompleted + totalFailures)) : 100.0;

        // Calculate average analysis duration
        List<ReviewHistory> histories = reviewHistoryRepository.findAll();
        long totalDurationMs = 0;
        int durationCount = 0;
        for (ReviewHistory rh : histories) {
            if ("COMPLETED".equals(rh.getStatus()) && rh.getCompletedAt() != null && rh.getCreatedAt() != null) {
                long duration = java.time.Duration.between(rh.getCreatedAt(), rh.getCompletedAt()).toMillis();
                totalDurationMs += duration;
                durationCount++;
            }
        }
        double avgDurationSec = durationCount > 0 ? (totalDurationMs / 1000.0 / durationCount) : 0.0;

        // Calculate average score
        List<Report> reports = reportRepository.findAll();
        double sumScore = 0;
        for (Report r : reports) {
            sumScore += r.getOverallScore();
        }
        double avgScore = reports.size() > 0 ? (sumScore / reports.size()) : 0.0;

        // Technology distributions
        Map<String, Integer> techs = new HashMap<>();
        Map<String, Integer> frameworks = new HashMap<>();
        for (Report r : reports) {
            String tech = r.getProject().getType(); // GITHUB/ZIP
            techs.put(tech, techs.getOrDefault(tech, 0) + 1);
        }

        // Active Users (users who uploaded at least one project)
        Set<Long> activeUserIds = new HashSet<>();
        for (Project p : projectRepository.findAll()) {
            if (p.getUser() != null) {
                activeUserIds.add(p.getUser().getId());
            }
        }

        // AI Request latency and stats
        List<AiUsageLog> usageLogs = aiUsageLogRepository.findAll();
        long totalAiCalls = usageLogs.size();
        long sumAiLatency = 0;
        for (AiUsageLog log : usageLogs) {
            sumAiLatency += log.getRequestTimeMs();
        }
        double avgAiLatency = totalAiCalls > 0 ? (sumAiLatency / (double) totalAiCalls) : 0.0;

        // Daily / Weekly / Monthly uploads count
        int daily = 0, weekly = 0, monthly = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Project p : projectRepository.findAll()) {
            if (p.getCreatedAt() != null) {
                if (p.getCreatedAt().isAfter(now.minusDays(1))) daily++;
                if (p.getCreatedAt().isAfter(now.minusWeeks(1))) weekly++;
                if (p.getCreatedAt().isAfter(now.minusMonths(1))) monthly++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("registeredUsers", totalUsers);
        data.put("activeUsers", activeUserIds.size());
        data.put("projectsUploaded", totalProjects);
        data.put("reportsGenerated", totalReports);
        data.put("totalAiRequests", totalAiCalls);
        data.put("averageAnalysisTime", Math.round(avgDurationSec * 10.0) / 10.0);
        data.put("averageAiResponseTime", Math.round((avgAiLatency / 1000.0) * 10.0) / 10.0);
        data.put("averageProjectScore", Math.round(avgScore * 10.0) / 10.0);
        data.put("failedAnalyses", totalFailures);
        data.put("successRate", Math.round(successRate * 10.0) / 10.0);
        data.put("technologyDistribution", techs);
        data.put("uploadsDaily", daily);
        data.put("uploadsWeekly", weekly);
        data.put("uploadsMonthly", monthly);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/ai-analytics")
    public ResponseEntity<?> getAiAnalytics() {
        List<AiUsageLog> logs = aiUsageLogRepository.findAll();
        double totalCost = 0.0;
        int totalTokens = 0;
        
        Map<String, Integer> providerCalls = new HashMap<>();
        Map<String, Double> providerCosts = new HashMap<>();
        Map<String, Integer> statusCalls = new HashMap<>();

        for (AiUsageLog log : logs) {
            totalCost += log.getCostEstimate();
            totalTokens += log.getTokensUsed();

            String prov = log.getProvider().toUpperCase();
            providerCalls.put(prov, providerCalls.getOrDefault(prov, 0) + 1);
            providerCosts.put(prov, providerCosts.getOrDefault(prov, 0.0) + log.getCostEstimate());

            String stat = log.getStatus().toUpperCase();
            statusCalls.put(stat, statusCalls.getOrDefault(stat, 0) + 1);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
        res.put("totalTokensUsed", totalTokens);
        res.put("providerCallsDistribution", providerCalls);
        res.put("providerCostsDistribution", providerCosts);
        res.put("statusDistribution", statusCalls);
        res.put("logs", logs);

        return ResponseEntity.ok(res);
    }

    // --- System Health Monitoring ---

    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // 1. DB status check
        boolean dbUp = false;
        try {
            userRepository.count();
            dbUp = true;
        } catch (Exception e) {
            // Down
        }
        health.put("databaseStatus", dbUp ? "UP" : "DOWN");
        health.put("backendStatus", "UP");
        health.put("aiProviderStatus", activeProvider != null ? "ACTIVE (" + activeProvider.toUpperCase() + ")" : "OFFLINE");

        // 2. Disk & workspace space
        File root = new File(".");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usableSpace = root.getUsableSpace();

        health.put("diskTotalGb", Math.round((totalSpace / 1073741824.0) * 10.0) / 10.0);
        health.put("diskFreeGb", Math.round((freeSpace / 1073741824.0) * 10.0) / 10.0);
        health.put("diskUsedPercent", Math.round(((totalSpace - freeSpace) * 100.0 / totalSpace) * 10.0) / 10.0);

        // 3. Memory metrics
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;

        health.put("memoryMaxMb", maxMem / 1048576);
        health.put("memoryTotalMb", totalMem / 1048576);
        health.put("memoryUsedMb", usedMem / 1048576);
        health.put("memoryUsedPercent", Math.round((usedMem * 100.0 / totalMem) * 10.0) / 10.0);

        // 4. Thread and CPU logs
        double systemLoad = java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        health.put("cpuSystemLoad", systemLoad >= 0 ? Math.round(systemLoad * 100.0) / 100.0 : "Unavailable");
        health.put("activeThreads", Thread.activeCount());

        return ResponseEntity.ok(health);
    }

    // --- System Log Tailer ---

    @GetMapping("/logs")
    public ResponseEntity<?> getLogs() {
        List<String> logLines = tailLogFile("logs/application.log", 150);
        return ResponseEntity.ok(logLines);
    }

    private List<String> tailLogFile(String path, int linesToRead) {
        List<String> list = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            list.add("Log file not found: " + file.getAbsolutePath());
            return list;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Queue<String> queue = new LinkedList<>();
            while ((line = br.readLine()) != null) {
                if (queue.size() >= linesToRead) {
                    queue.poll();
                }
                queue.add(line);
            }
            list.addAll(queue);
        } catch (Exception e) {
            list.add("Error reading log file: " + e.getMessage());
        }
        return list;
    }

    // --- System Backups ---

    @GetMapping("/backup/download")
    public ResponseEntity<byte[]> downloadBackup(Authentication authentication, HttpServletRequest request) {
        try {
            byte[] backupZipBytes = backupService.createBackupArchive();
            
            auditLogService.log(authentication.getName(), "System Backup", "SUCCESS", getClientIp(request),
                    "Created and downloaded complete database and projects backup zip archive.");

            String filename = "devspec-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

            return new ResponseEntity<>(backupZipBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
