package com.devspec.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class WorkspaceManager {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceManager.class);

    private final String uploadBaseDir;

    public WorkspaceManager(@Value("${devspec.upload.dir}") String uploadBaseDir) {
        this.uploadBaseDir = uploadBaseDir;
    }

    public File createWorkspace() throws IOException {
        File baseDir = new File(uploadBaseDir);
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                throw new IOException("Failed to create base upload directory: " + baseDir.getAbsolutePath());
            }
        }

        String uniqueWorkspaceId = "workspace_" + UUID.randomUUID().toString();
        File workspaceDir = new File(baseDir, uniqueWorkspaceId);
        if (!workspaceDir.exists()) {
            if (!workspaceDir.mkdirs()) {
                throw new IOException("Failed to create isolated workspace directory: " + workspaceDir.getAbsolutePath());
            }
        }

        logger.info("Created secure isolated workspace: {}", workspaceDir.getAbsolutePath());
        return workspaceDir;
    }

    public void cleanWorkspace(File workspaceDir) {
        if (workspaceDir != null && workspaceDir.exists()) {
            boolean deleted = FileSystemUtils.deleteRecursively(workspaceDir);
            if (deleted) {
                logger.info("Successfully cleaned workspace directory: {}", workspaceDir.getAbsolutePath());
            } else {
                logger.warn("Failed to recursively delete workspace directory: {}", workspaceDir.getAbsolutePath());
            }
        }
    }
}
