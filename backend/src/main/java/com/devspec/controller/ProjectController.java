package com.devspec.controller;

import com.devspec.exception.BadRequestException;
import com.devspec.exception.ResourceNotFoundException;
import com.devspec.model.*;
import com.devspec.repository.*;
import com.devspec.service.ProjectPipelineService;
import com.devspec.service.analysis.FileExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final ReportRepository reportRepository;
    private final FileExtractionService fileExtractionService;
    private final ProjectPipelineService pipelineService;
    private final com.devspec.service.ProgressRegistryService progressRegistryService;
    private final com.devspec.service.AuditLogService auditLogService;
    private final com.devspec.service.SystemSettingService systemSettingService;
    private final com.devspec.service.analysis.GitCloningService gitCloningService;

    public ProjectController(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            ReportRepository reportRepository,
            FileExtractionService fileExtractionService,
            ProjectPipelineService pipelineService,
            com.devspec.service.ProgressRegistryService progressRegistryService,
            com.devspec.service.AuditLogService auditLogService,
            com.devspec.service.SystemSettingService systemSettingService,
            com.devspec.service.analysis.GitCloningService gitCloningService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.reportRepository = reportRepository;
        this.fileExtractionService = fileExtractionService;
        this.pipelineService = pipelineService;
        this.progressRegistryService = progressRegistryService;
        this.auditLogService = auditLogService;
        this.systemSettingService = systemSettingService;
        this.gitCloningService = gitCloningService;
    }

    @PostMapping(value = "/upload/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadZip(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        logger.info("User '{}' requested ZIP project upload: {}", user.getUsername(), file.getOriginalFilename());

        // Dynamic file size check
        long maxBytes = Long.parseLong(systemSettingService.getSetting("max_upload_size", "52428800"));
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File size exceeds the configured maximum upload limit of " + (maxBytes / (1024 * 1024)) + "MB");
        }

        // 1. Initial file validations
        File savedZip = fileExtractionService.validateAndSaveZip(file);

        // 2. Create Project Entity
        Project project = Project.builder()
                .name(file.getOriginalFilename().replace(".zip", ""))
                .type("ZIP")
                .filePath(savedZip.getAbsolutePath())
                .user(user)
                .build();
        project = projectRepository.save(project);

        // Log upload audit
        auditLogService.log(
                user.getUsername(),
                "Project Upload",
                "SUCCESS",
                "Uploaded project: " + project.getName() + " via ZIP archive"
        );

        // 3. Initialize History
        ReviewHistory history = pipelineService.initializePipelineRun(project);

        // 4. Trigger Async Pipeline
        pipelineService.executeZipPipeline(history.getAnalysisId(), savedZip);

        Map<String, Object> response = new HashMap<>();
        response.put("analysisId", history.getAnalysisId());
        response.put("projectId", project.getId());
        response.put("status", history.getStatus());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload/git")
    public ResponseEntity<?> uploadGit(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        String repoUrl = request.get("repoUrl");
        String token = request.get("token"); // Optional personal access token

        logger.info("User '{}' requested Git repository clone: {}", user.getUsername(), repoUrl);

        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            throw new BadRequestException("Repository URL is required");
        }

        String projectName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1).replace(".git", "");

        // 2. Create Project Entity
        Project project = Project.builder()
                .name(projectName)
                .type("GITHUB")
                .repoUrl(repoUrl)
                .user(user)
                .build();
        project = projectRepository.save(project);

        // Log upload audit
        auditLogService.log(
                user.getUsername(),
                "Project Upload",
                "SUCCESS",
                "Uploaded project: " + project.getName() + " via Git URL: " + repoUrl
        );

        // 3. Initialize History
        ReviewHistory history = pipelineService.initializePipelineRun(project);

        // Store chosen branch, commit or tag in the history run
        String branch = request.get("branch");
        String commit = request.get("commit");
        String tag = request.get("tag");
        
        history.setRepositoryName(projectName);
        history.setBranch(branch != null && !branch.trim().isEmpty() ? branch : "main");
        history.setCommitHash(commit != null && !commit.trim().isEmpty() ? commit : null);
        history.setGitTag(tag != null && !tag.trim().isEmpty() ? tag : null);
        history = reviewHistoryRepository.save(history);

        // 4. Trigger Async Pipeline
        pipelineService.executeGitPipeline(history.getAnalysisId(), repoUrl, token);

        Map<String, Object> response = new HashMap<>();
        response.put("analysisId", history.getAnalysisId());
        response.put("projectId", project.getId());
        response.put("status", history.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/progress/{analysisId}")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamProgress(
            @PathVariable String analysisId) {
        return progressRegistryService.register(analysisId);
    }

    @PostMapping("/analysis/cancel/{analysisId}")
    public ResponseEntity<?> cancelAnalysis(
            @PathVariable String analysisId,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        ReviewHistory history = reviewHistoryRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis run not found for ID: " + analysisId));

        if (!history.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to cancel this run.");
        }

        // Set status to CANCELLED. The running async thread checks this and aborts
        history.setStatus("CANCELLED");
        history.setStage("Cancelled");
        reviewHistoryRepository.save(history);

        logger.info("User '{}' requested cancellation for run: {}", user.getUsername(), analysisId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("analysisId", analysisId);
        response.put("status", "CANCELLED");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/analysis/{analysisId}")
    public ResponseEntity<?> getStatusByAnalysisId(@PathVariable String analysisId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        ReviewHistory history = reviewHistoryRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("History not found with Analysis ID: " + analysisId));

        if (!history.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to project history");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("analysisId", history.getAnalysisId());
        response.put("projectId", history.getProject().getId());
        response.put("status", history.getStatus());
        response.put("stage", history.getStage());
        response.put("errorMessage", history.getErrorMessage());
        
        if (history.getReport() != null) {
            response.put("reportId", history.getReport().getId());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{historyId}")
    public ResponseEntity<?> getStatus(@PathVariable Long historyId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        ReviewHistory history = reviewHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("History not found with ID: " + historyId));

        if (!history.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to project history");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("historyId", history.getId());
        response.put("analysisId", history.getAnalysisId());
        response.put("projectId", history.getProject().getId());
        response.put("status", history.getStatus());
        response.put("stage", history.getStage());
        response.put("errorMessage", history.getErrorMessage());
        
        if (history.getReport() != null) {
            response.put("reportId", history.getReport().getId());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String framework,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Double maxScore,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String technology,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        List<ReviewHistory> historyList = reviewHistoryRepository.findByProjectUserIdOrderByCreatedAtDesc(user.getId());
        
        List<Map<String, Object>> filteredList = new ArrayList<>();
        
        for (ReviewHistory rh : historyList) {
            boolean matches = true;
            
            // Search filter
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                boolean nameMatch = rh.getProject().getName().toLowerCase().contains(searchLower);
                boolean langMatch = false;
                boolean fwMatch = false;
                
                if (rh.getAnalysisResult() != null) {
                    if (rh.getAnalysisResult().getLanguage() != null) {
                        langMatch = rh.getAnalysisResult().getLanguage().toLowerCase().contains(searchLower);
                    }
                    if (rh.getAnalysisResult().getFramework() != null) {
                        fwMatch = rh.getAnalysisResult().getFramework().toLowerCase().contains(searchLower);
                    }
                }
                matches = nameMatch || langMatch || fwMatch;
            }

            // Language filter
            if (matches && language != null && !language.trim().isEmpty()) {
                if (rh.getAnalysisResult() != null && rh.getAnalysisResult().getLanguage() != null) {
                    matches = rh.getAnalysisResult().getLanguage().equalsIgnoreCase(language.trim());
                } else {
                    matches = false;
                }
            }

            // Framework filter
            if (matches && framework != null && !framework.trim().isEmpty()) {
                if (rh.getAnalysisResult() != null && rh.getAnalysisResult().getFramework() != null) {
                    matches = rh.getAnalysisResult().getFramework().toLowerCase().contains(framework.toLowerCase().trim());
                } else {
                    matches = false;
                }
            }

            // Min Score filter
            if (matches && minScore != null) {
                if (rh.getReport() != null) {
                    matches = rh.getReport().getOverallScore() >= minScore;
                } else {
                    matches = false;
                }
            }

            // Max Score filter
            if (matches && maxScore != null) {
                if (rh.getReport() != null) {
                    matches = rh.getReport().getOverallScore() <= maxScore;
                } else {
                    matches = false;
                }
            }

            // Technology filter
            if (matches && technology != null && !technology.trim().isEmpty()) {
                matches = rh.getProject().getType().equalsIgnoreCase(technology.trim());
            }

            // Tag filter
            if (matches && tag != null && !tag.trim().isEmpty()) {
                matches = rh.getProject().getTags().stream().anyMatch(t -> t.equalsIgnoreCase(tag.trim()));
            }

            if (matches) {
                Map<String, Object> item = new HashMap<>();
                item.put("historyId", rh.getId());
                item.put("projectId", rh.getProject().getId());
                item.put("projectName", rh.getProject().getName());
                item.put("projectType", rh.getProject().getType());
                item.put("status", rh.getStatus());
                item.put("stage", rh.getStage());
                item.put("createdAt", rh.getCreatedAt());
                item.put("tags", rh.getProject().getTags());
                
                // Add metric details for frontend filter ease
                if (rh.getAnalysisResult() != null) {
                    item.put("language", rh.getAnalysisResult().getLanguage());
                    item.put("framework", rh.getAnalysisResult().getFramework());
                }
                
                if (rh.getReport() != null) {
                    item.put("overallScore", rh.getReport().getOverallScore());
                    item.put("reportId", rh.getReport().getId());
                } else {
                    item.put("overallScore", 0.0);
                }
                filteredList.add(item);
            }
        }

        // Sorting
        boolean desc = "desc".equalsIgnoreCase(sortOrder);
        filteredList.sort((m1, m2) -> {
            int comp = 0;
            if ("name".equalsIgnoreCase(sortBy)) {
                String n1 = (String) m1.get("projectName");
                String n2 = (String) m2.get("projectName");
                comp = n1.compareToIgnoreCase(n2);
            } else if ("score".equalsIgnoreCase(sortBy)) {
                Double s1 = (Double) m1.get("overallScore");
                Double s2 = (Double) m2.get("overallScore");
                comp = Double.compare(s1 != null ? s1 : 0.0, s2 != null ? s2 : 0.0);
            } else { // default date
                java.time.LocalDateTime d1 = (java.time.LocalDateTime) m1.get("createdAt");
                java.time.LocalDateTime d2 = (java.time.LocalDateTime) m2.get("createdAt");
                comp = d1.compareTo(d2);
            }
            return desc ? -comp : comp;
        });

        return ResponseEntity.ok(filteredList);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<ReviewHistory> historyList = reviewHistoryRepository.findByProjectUserIdOrderByCreatedAtDesc(user.getId());

        long totalReviews = reviewHistoryRepository.countByProjectUserId(user.getId());
        double sumScore = 0.0;
        int scoreCount = 0;

        List<Map<String, Object>> recentUploads = new ArrayList<>();
        int count = 0;

        for (ReviewHistory rh : historyList) {
            if (rh.getReport() != null) {
                sumScore += rh.getReport().getOverallScore();
                scoreCount++;
            }
            if (count < 5) {
                Map<String, Object> item = new HashMap<>();
                item.put("projectId", rh.getProject().getId());
                item.put("name", rh.getProject().getName());
                item.put("type", rh.getProject().getType());
                item.put("status", rh.getStatus());
                item.put("date", rh.getCreatedAt());
                recentUploads.add(item);
                count++;
            }
        }

        double averageScore = scoreCount > 0 ? (sumScore / scoreCount) : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", totalReviews);
        stats.put("averageScore", Math.round(averageScore * 10.0) / 10.0);
        stats.put("recentUploads", recentUploads);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/report/{projectId}")
    public ResponseEntity<?> getReportByProject(@PathVariable Long projectId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));
        
        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to project details");
        }

        Report report = reportRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found for Project: " + projectId));

        // We also want to fetch metrics and test results to display in tabs
        ReviewHistory latestRun = reviewHistoryRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().filter(rh -> rh.getStatus().equals("COMPLETED"))
                .findFirst().orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", report.getId());
        response.put("projectId", project.getId());
        response.put("projectName", project.getName());
        response.put("projectType", project.getType());
        
        // Scores
        response.put("overallScore", report.getOverallScore());
        response.put("codeQualityScore", report.getCodeQualityScore());
        response.put("documentationScore", report.getDocumentationScore());
        response.put("maintainabilityScore", report.getMaintainabilityScore());
        
        // Summaries
        response.put("architectureSummary", report.getArchitectureSummary());
        response.put("techStack", report.getTechStack());
        response.put("securityAnalysis", report.getSecurityAnalysis());
        response.put("finalVerdict", report.getFinalVerdict());
        
        // JSON Arrays
        response.put("strengths", report.getStrengthsJson());
        response.put("weaknesses", report.getWeaknessesJson());
        response.put("aiSuggestions", report.getAiSuggestionsJson());
        response.put("createdAt", report.getCreatedAt());

        if (latestRun != null) {
            if (latestRun.getAnalysisResult() != null) {
                response.put("analysisMetrics", new com.fasterxml.jackson.databind.util.RawValue(latestRun.getAnalysisResult().getStaticAnalysisIssuesJson()));
                
                Map<String, Object> struct = new HashMap<>();
                struct.put("language", latestRun.getAnalysisResult().getLanguage());
                struct.put("buildTool", latestRun.getAnalysisResult().getBuildTool());
                struct.put("framework", latestRun.getAnalysisResult().getFramework());
                struct.put("numPackages", latestRun.getAnalysisResult().getNumPackages());
                struct.put("numClasses", latestRun.getAnalysisResult().getNumClasses());
                struct.put("numInterfaces", latestRun.getAnalysisResult().getNumInterfaces());
                struct.put("numEnums", latestRun.getAnalysisResult().getNumEnums());
                struct.put("numRecords", latestRun.getAnalysisResult().getNumRecords());
                struct.put("numMethods", latestRun.getAnalysisResult().getNumMethods());
                struct.put("numConstructors", latestRun.getAnalysisResult().getNumConstructors());
                struct.put("numFields", latestRun.getAnalysisResult().getNumFields());
                response.put("structuralMetrics", struct);
            }
            if (latestRun.getUnitTestResult() != null) {
                response.put("unitTestResult", new com.fasterxml.jackson.databind.util.RawValue(latestRun.getUnitTestResult().getFailureDetailsJson()));
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{reportId}")
    public ResponseEntity<Resource> downloadReportPdf(@PathVariable Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        if (report.getPdfReportPath() == null) {
            throw new BadRequestException("PDF report file is not available");
        }

        File file = new File(report.getPdfReportPath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("PDF report file does not exist on disk");
        }

        Resource resource = new FileSystemResource(file);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @GetMapping("/git/branches")
    public ResponseEntity<?> getBranches(
            @RequestParam("repoUrl") String repoUrl,
            @RequestParam(value = "token", required = false) String token) {
        logger.info("Fetching remote branches for repository: {}", repoUrl);
        return ResponseEntity.ok(gitCloningService.getRemoteBranches(repoUrl, token));
    }

    @GetMapping("/git/tags")
    public ResponseEntity<?> getTags(
            @RequestParam("repoUrl") String repoUrl,
            @RequestParam(value = "token", required = false) String token) {
        logger.info("Fetching remote tags for repository: {}", repoUrl);
        return ResponseEntity.ok(gitCloningService.getRemoteTags(repoUrl, token));
    }

    @GetMapping("/git/commits")
    public ResponseEntity<?> getCommits(
            @RequestParam("repoUrl") String repoUrl,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam("branchOrTag") String branchOrTag) {
        logger.info("Fetching remote commits for repository: {} (ref: {})", repoUrl, branchOrTag);
        return ResponseEntity.ok(gitCloningService.getRemoteCommits(repoUrl, token, branchOrTag));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new BadRequestException("User not authenticated");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadRequestException("Authenticated user details not found"));
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<?> addTag(@PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User user = getCurrentUser(authentication);
        if (!project.getUser().getId().equals(user.getId()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: You do not own this project.");
        }

        String tag = payload.get("tag");
        if (tag == null || tag.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Tag cannot be empty");
        }

        project.getTags().add(tag.trim().toLowerCase());
        projectRepository.save(project);

        return ResponseEntity.ok(Map.of("message", "Tag added successfully", "tags", project.getTags()));
    }

    @DeleteMapping("/{id}/tags/{tag}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id, @PathVariable String tag, Authentication authentication) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User user = getCurrentUser(authentication);
        if (!project.getUser().getId().equals(user.getId()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: You do not own this project.");
        }

        project.getTags().remove(tag.trim().toLowerCase());
        projectRepository.save(project);

        return ResponseEntity.ok(Map.of("message", "Tag removed successfully", "tags", project.getTags()));
    }
}
