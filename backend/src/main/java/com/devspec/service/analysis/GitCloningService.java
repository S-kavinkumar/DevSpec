package com.devspec.service.analysis;

import com.devspec.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

@Service
public class GitCloningService {
    private static final Logger logger = LoggerFactory.getLogger(GitCloningService.class);

    private final String uploadBaseDir;

    // Pattern to validate git repository URL
    private static final Pattern GIT_URL_PATTERN = Pattern.compile(
            "^(https://|git@)(github\\.com|gitlab\\.com|bitbucket\\.org)/[\\w.-]+/[\\w.-]+(\\.git)?$"
    );

    public GitCloningService(@Value("${devspec.upload.dir}") String uploadBaseDir) {
        this.uploadBaseDir = uploadBaseDir;
    }

    public void validateGitUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new BadRequestException("Git repository URL cannot be empty");
        }
        String trimmed = url.trim();
        if (!GIT_URL_PATTERN.matcher(trimmed).matches()) {
            throw new BadRequestException("Invalid Git repository URL. Supported providers: GitHub, GitLab, and Bitbucket.");
        }
    }

    public File cloneRepository(String repoUrl, String personalAccessToken) {
        validateGitUrl(repoUrl);
        String uniqueId = UUID.randomUUID().toString();
        File cloneDir = new File(uploadBaseDir, uniqueId);
        if (!cloneDir.exists()) {
            cloneDir.mkdirs();
        }
        cloneRepositoryToDir(repoUrl, personalAccessToken, cloneDir);
        return cloneDir;
    }

    public void cloneRepositoryToDir(String repoUrl, String personalAccessToken, File cloneDir) {
        validateGitUrl(repoUrl);

        // Build authenticated URL if token is provided
        String authenticatedUrl = buildAuthenticatedUrl(repoUrl, personalAccessToken);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "clone", authenticatedUrl, cloneDir.getAbsolutePath());
        processBuilder.redirectErrorStream(true);

        try {
            logger.info("Starting git clone for repository: {} into {}", repoUrl, cloneDir.getAbsolutePath());
            Process process = processBuilder.start();
            
            // Read output log to debug
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[GIT] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BadRequestException("Git clone failed. Make sure the repository is public and accessible.");
            }

            logger.info("Successfully cloned Git repository to: {}", cloneDir.getAbsolutePath());
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing git clone for URL: {}", repoUrl, e);
            // Cleanup directory if it failed
            FileSystemUtilsDelete(cloneDir);
            throw new BadRequestException("Git clone execution failed: " + e.getMessage());
        }
    }

    private String buildAuthenticatedUrl(String repoUrl, String token) {
        if (token == null || token.trim().isEmpty()) {
            return repoUrl;
        }
        
        // E.g., https://github.com/user/repo.git -> https://<token>@github.com/user/repo.git
        if (repoUrl.startsWith("https://")) {
            return "https://" + token.trim() + "@" + repoUrl.substring(8);
        }
        
        return repoUrl;
    }

    private void FileSystemUtilsDelete(File dir) {
        if (dir.exists()) {
            org.springframework.util.FileSystemUtils.deleteRecursively(dir);
        }
    }

    public List<Map<String, String>> getRemoteBranches(String repoUrl, String token) {
        validateGitUrl(repoUrl);
        String authenticatedUrl = buildAuthenticatedUrl(repoUrl, token);
        return executeLsRemote(authenticatedUrl, "--heads", "refs/heads/");
    }

    public List<Map<String, String>> getRemoteTags(String repoUrl, String token) {
        validateGitUrl(repoUrl);
        String authenticatedUrl = buildAuthenticatedUrl(repoUrl, token);
        return executeLsRemote(authenticatedUrl, "--tags", "refs/tags/");
    }

    private List<Map<String, String>> executeLsRemote(String url, String typeFlag, String refPrefix) {
        List<Map<String, String>> refs = new ArrayList<>();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "ls-remote", typeFlag, url);
        processBuilder.redirectErrorStream(true);
        try {
            logger.info("Executing git ls-remote {} for URL: {}", typeFlag, url);
            Process process = processBuilder.start();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String hash = parts[0];
                        String ref = parts[1];
                        if (ref.startsWith(refPrefix)) {
                            String name = ref.substring(refPrefix.length());
                            if (name.endsWith("^{}")) {
                                continue; // skip dereferenced tags
                            }
                            Map<String, String> refMap = new HashMap<>();
                            refMap.put("name", name);
                            refMap.put("commitHash", hash);
                            refs.add(refMap);
                        }
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BadRequestException("Git remote query failed. Ensure repository URL is valid and token is correct.");
            }
        } catch (Exception e) {
            logger.error("Failed to run ls-remote on URL: {}", url, e);
            throw new BadRequestException("Failed to fetch remote repository references: " + e.getMessage());
        }
        return refs;
    }

    public List<Map<String, String>> getRemoteCommits(String repoUrl, String token, String branchOrTag) {
        validateGitUrl(repoUrl);
        String authenticatedUrl = buildAuthenticatedUrl(repoUrl, token);
        String uniqueId = UUID.randomUUID().toString();
        File tempDir = new File(uploadBaseDir, "temp_git_" + uniqueId);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        List<Map<String, String>> commits = new ArrayList<>();
        try {
            logger.info("Shallow bare cloning remote repo: {} for commits, branch: {}", repoUrl, branchOrTag);
            ProcessBuilder clonePb = new ProcessBuilder();
            clonePb.command("git", "clone", "--bare", "--depth=50", "--single-branch", "--branch=" + branchOrTag, authenticatedUrl, tempDir.getAbsolutePath());
            clonePb.redirectErrorStream(true);
            Process cloneProc = clonePb.start();
            int cloneExit = cloneProc.waitFor();
            if (cloneExit != 0) {
                throw new BadRequestException("Shallow clone failed. Ensure branch or tag exists.");
            }

            ProcessBuilder logPb = new ProcessBuilder();
            logPb.command("git", "log", "-n", "50", "--pretty=format:%H|%an|%ad|%s");
            logPb.directory(tempDir);
            Process logProc = logPb.start();
            
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(logProc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", 4);
                    if (parts.length >= 4) {
                        Map<String, String> commit = new HashMap<>();
                        commit.put("commitHash", parts[0]);
                        commit.put("author", parts[1]);
                        commit.put("date", parts[2]);
                        commit.put("message", parts[3]);
                        commits.add(commit);
                    }
                }
            }
            logProc.waitFor();
        } catch (Exception e) {
            logger.error("Failed to fetch remote commits for repo: {} on branch: {}", repoUrl, branchOrTag, e);
            throw new BadRequestException("Failed to fetch commits. Make sure the branch/tag exists: " + e.getMessage());
        } finally {
            if (tempDir.exists()) {
                org.springframework.util.FileSystemUtils.deleteRecursively(tempDir);
            }
        }
        return commits;
    }

    public void checkoutRef(File cloneDir, String ref) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "checkout", ref);
        processBuilder.directory(cloneDir);
        processBuilder.redirectErrorStream(true);
        try {
            logger.info("Executing git checkout {} in {}", ref, cloneDir.getAbsolutePath());
            Process process = processBuilder.start();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[GIT CHECKOUT] {}", line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BadRequestException("Git checkout failed for ref: " + ref);
            }
        } catch (IOException | InterruptedException e) {
            throw new BadRequestException("Failed to checkout ref: " + ref + ". Error: " + e.getMessage());
        }
    }

    public String getHeadCommitHash(File cloneDir) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("git", "rev-parse", "HEAD");
        processBuilder.directory(cloneDir);
        try {
            Process process = processBuilder.start();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    return line.trim();
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.warn("Failed to get HEAD commit hash", e);
        }
        return null;
    }
}
