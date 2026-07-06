package com.devspec.service;

import com.devspec.model.ReviewHistory;
import com.devspec.repository.ReviewHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemHealthService {
    private static final Logger logger = LoggerFactory.getLogger(SystemHealthService.class);

    private final ReviewHistoryRepository reviewHistoryRepository;
    private final String uploadBaseDir;
    private final String activeProvider;
    private final String geminiKey;
    private final String groqKey;

    public SystemHealthService(
            ReviewHistoryRepository reviewHistoryRepository,
            @Value("${devspec.upload.dir}") String uploadBaseDir,
            @Value("${ai.provider:gemini}") String activeProvider,
            @Value("${gemini.api.key:}") String geminiKey,
            @Value("${groq.api.key:}") String groqKey) {
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.uploadBaseDir = uploadBaseDir;
        this.activeProvider = activeProvider;
        this.geminiKey = geminiKey;
        this.groqKey = groqKey;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        // 1. Database Status
        String dbStatus = "UP";
        try {
            reviewHistoryRepository.count();
        } catch (Exception e) {
            dbStatus = "DOWN";
            logger.error("Database healthcheck failed: {}", e.getMessage());
        }

        // 2. AI Provider Status
        String aiStatus = "UP";
        String configuredKey = "gemini".equalsIgnoreCase(activeProvider) ? geminiKey : groqKey;
        if (configuredKey == null || configuredKey.trim().isEmpty()) {
            aiStatus = "DEGRADED (API Key missing)";
        }

        // 3. Disk Usage
        File uploadDir = new File(uploadBaseDir);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        long totalSpace = uploadDir.getTotalSpace();
        long freeSpace = uploadDir.getFreeSpace();
        long usableSpace = uploadDir.getUsableSpace();

        // 4. Temporary Workspace Usage (recursively calculate folder size)
        long workspaceSize = calculateFolderSize(uploadDir);

        // 5. Queue metrics
        List<String> inProgressStatuses = Arrays.asList(
                "QUEUED", "VALIDATING", "EXTRACTING", "ANALYZING", 
                "STATIC_ANALYSIS", "RUNNING_TESTS", "GENERATING_AI_REVIEW", "GENERATING_REPORT",
                "ANALYZING_PROJECT", "RUNNING_STATIC_ANALYSIS", "EXECUTING_UNIT_TESTS", "CREATING_FINAL_REPORT"
        );
        long queueSize = reviewHistoryRepository.findAll().stream()
                .filter(rh -> inProgressStatuses.contains(rh.getStatus().toUpperCase()))
                .count();

        // 6. Compute Average Analysis Time (for completed histories)
        double avgAnalysisSeconds = 0.0;
        List<ReviewHistory> completedRuns = reviewHistoryRepository.findAll().stream()
                .filter(rh -> "COMPLETED".equalsIgnoreCase(rh.getStatus()) && rh.getCompletedAt() != null)
                .toList();

        if (!completedRuns.isEmpty()) {
            long totalSeconds = 0;
            for (ReviewHistory rh : completedRuns) {
                Duration duration = Duration.between(rh.getCreatedAt(), rh.getCompletedAt());
                totalSeconds += duration.getSeconds();
            }
            avgAnalysisSeconds = (double) totalSeconds / completedRuns.size();
        }

        health.put("systemStatus", "UP");
        health.put("databaseStatus", dbStatus);
        health.put("aiProvider", activeProvider.toUpperCase());
        health.put("aiProviderStatus", aiStatus);
        health.put("diskTotalBytes", totalSpace);
        health.put("diskFreeBytes", freeSpace);
        health.put("diskUsableBytes", usableSpace);
        health.put("workspaceUsageBytes", workspaceSize);
        health.put("queueSize", queueSize);
        health.put("avgAnalysisSeconds", Math.round(avgAnalysisSeconds * 10.0) / 10.0);
        health.put("avgReportGenerationSeconds", Math.round((avgAnalysisSeconds * 0.15) * 10.0) / 10.0); // Estimate report compilation at ~15% of pipeline

        return health;
    }

    private long calculateFolderSize(File directory) {
        long length = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += calculateFolderSize(file);
                }
            }
        }
        return length;
    }
}
