package com.devspec.service.analysis;

import com.devspec.model.ReviewHistory;
import com.devspec.repository.ReviewHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class WorkspaceStartupCleaner {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceStartupCleaner.class);

    private final ReviewHistoryRepository reviewHistoryRepository;
    private final String uploadBaseDir;

    public WorkspaceStartupCleaner(
            ReviewHistoryRepository reviewHistoryRepository,
            @Value("${devspec.upload.dir}") String uploadBaseDir) {
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.uploadBaseDir = uploadBaseDir;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        logger.info("Initializing system startup recovery and workspace cleaning routines...");

        // 1. Recover interrupted status jobs in the database
        recoverInterruptedRuns();

        // 2. Clean temporary files in base upload folder
        cleanTemporaryUploads();
    }

    private void recoverInterruptedRuns() {
        List<String> activeStatuses = Arrays.asList(
                "QUEUED", "VALIDATING", "EXTRACTING", "ANALYZING", "STATIC_ANALYSIS", 
                "RUNNING_TESTS", "GENERATING_AI_REVIEW", "GENERATING_REPORT",
                "ANALYZING_PROJECT", "RUNNING_STATIC_ANALYSIS", "EXECUTING_UNIT_TESTS", 
                "CREATING_FINAL_REPORT", "UPLOADING"
        );

        try {
            List<ReviewHistory> interrupted = reviewHistoryRepository.findAll().stream()
                    .filter(rh -> activeStatuses.contains(rh.getStatus().toUpperCase()))
                    .toList();

            if (!interrupted.isEmpty()) {
                logger.info("Detected {} interrupted analysis runs. Recovering tasks to FAILED state.", interrupted.size());
                for (ReviewHistory rh : interrupted) {
                    rh.setStatus("FAILED");
                    rh.setStage("Failed");
                    rh.setErrorMessage("Analysis interrupted due to unexpected system shutdown.");
                    rh.setCompletedAt(LocalDateTime.now());
                    reviewHistoryRepository.save(rh);
                }
            } else {
                logger.info("No interrupted analysis runs found.");
            }
        } catch (Exception e) {
            logger.error("Failed to recover interrupted history runs: {}", e.getMessage(), e);
        }
    }

    private void cleanTemporaryUploads() {
        File uploadDir = new File(uploadBaseDir);
        if (!uploadDir.exists() || !uploadDir.isDirectory()) {
            return;
        }

        File[] files = uploadDir.listFiles();
        if (files == null) {
            return;
        }

        logger.info("Scanning for orphaned workspaces and temporary ZIP archives in {}", uploadDir.getAbsolutePath());
        int count = 0;
        for (File f : files) {
            // Cleanup zip files and folder workspaces
            if (f.isFile() && f.getName().endsWith(".zip")) {
                if (f.delete()) count++;
            } else if (f.isDirectory() && f.getName().startsWith("workspace_")) {
                if (FileSystemUtils.deleteRecursively(f)) count++;
            }
        }
        logger.info("Successfully purged {} orphaned workspaces/archives.", count);
    }
}
