package com.devspec.service;

import com.devspec.model.*;
import com.devspec.repository.*;
import com.devspec.service.ai.AIReviewService;
import com.devspec.service.analysis.FileExtractionService;
import com.devspec.service.analysis.GitCloningService;
import com.devspec.service.analysis.ProjectAnalysisService;
import com.devspec.service.analysis.StaticCodeAnalysisService;
import com.devspec.service.analysis.WorkspaceManager;
import com.devspec.service.report.ReportGenerationService;
import com.devspec.service.testing.UnitTestExecutionService;
import com.devspec.service.analysis.DependencyAnalysisService;
import com.devspec.service.analysis.ConfigurationAnalysisService;
import com.devspec.service.analysis.ApiDocumentationAnalysisService;
import com.devspec.service.analysis.DatabaseAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProjectPipelineService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectPipelineService.class);

    private final ProjectRepository projectRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UnitTestResultRepository unitTestResultRepository;
    private final ReportRepository reportRepository;

    private final FileExtractionService fileExtractionService;
    private final GitCloningService gitCloningService;
    private final ProjectAnalysisService projectAnalysisService;
    private final StaticCodeAnalysisService staticCodeAnalysisService;
    private final UnitTestExecutionService unitTestExecutionService;
    private final AIReviewService aiReviewService;
    private final ReportGenerationService reportGenerationService;
    
    private final ProgressRegistryService progressRegistryService;
    private final AuditLogService auditLogService;
    private final WorkspaceManager workspaceManager;

    private final DependencyAnalysisService dependencyAnalysisService;
    private final ConfigurationAnalysisService configurationAnalysisService;
    private final ApiDocumentationAnalysisService apiDocumentationAnalysisService;
    private final DatabaseAnalysisService databaseAnalysisService;
    private final SystemSettingService systemSettingService;

    public ProjectPipelineService(
            ProjectRepository projectRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            AnalysisResultRepository analysisResultRepository,
            UnitTestResultRepository unitTestResultRepository,
            ReportRepository reportRepository,
            FileExtractionService fileExtractionService,
            GitCloningService gitCloningService,
            ProjectAnalysisService projectAnalysisService,
            StaticCodeAnalysisService staticCodeAnalysisService,
            UnitTestExecutionService unitTestExecutionService,
            AIReviewService aiReviewService,
            ReportGenerationService reportGenerationService,
            ProgressRegistryService progressRegistryService,
            AuditLogService auditLogService,
            WorkspaceManager workspaceManager,
            DependencyAnalysisService dependencyAnalysisService,
            ConfigurationAnalysisService configurationAnalysisService,
            ApiDocumentationAnalysisService apiDocumentationAnalysisService,
            DatabaseAnalysisService databaseAnalysisService,
            SystemSettingService systemSettingService) {
        this.projectRepository = projectRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.unitTestResultRepository = unitTestResultRepository;
        this.reportRepository = reportRepository;
        this.fileExtractionService = fileExtractionService;
        this.gitCloningService = gitCloningService;
        this.projectAnalysisService = projectAnalysisService;
        this.staticCodeAnalysisService = staticCodeAnalysisService;
        this.unitTestExecutionService = unitTestExecutionService;
        this.aiReviewService = aiReviewService;
        this.reportGenerationService = reportGenerationService;
        this.progressRegistryService = progressRegistryService;
        this.auditLogService = auditLogService;
        this.workspaceManager = workspaceManager;
        this.dependencyAnalysisService = dependencyAnalysisService;
        this.configurationAnalysisService = configurationAnalysisService;
        this.apiDocumentationAnalysisService = apiDocumentationAnalysisService;
        this.databaseAnalysisService = databaseAnalysisService;
        this.systemSettingService = systemSettingService;
    }

    public boolean isProjectAnalyzing(Long projectId) {
        List<ReviewHistory> histories = reviewHistoryRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return histories.stream().anyMatch(h -> 
            !"COMPLETED".equals(h.getStatus()) && 
            !"FAILED".equals(h.getStatus()) && 
            !"CANCELLED".equals(h.getStatus())
        );
    }

    public ReviewHistory initializePipelineRun(Project project) {
        if (isProjectAnalyzing(project.getId())) {
            throw new IllegalStateException("An active analysis pipeline run is already in progress for project: " + project.getName());
        }

        String analysisId = UUID.randomUUID().toString();
        
        ReviewHistory history = ReviewHistory.builder()
                .project(project)
                .analysisId(analysisId)
                .status("QUEUED")
                .stage("Queued in Analysis Pool")
                .build();
        
        history = reviewHistoryRepository.save(history);
        
        // Audit log start
        auditLogService.log(
                project.getUser().getUsername(),
                "Analysis Start",
                "SUCCESS",
                "Initialized analysis run version for project: " + project.getName() + " (Analysis ID: " + analysisId + ")"
        );
        
        return history;
    }

    @Async("analysisTaskExecutor")
    public CompletableFuture<Void> executeZipPipeline(String analysisId, File zipFile) {
        ReviewHistory history = reviewHistoryRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new RuntimeException("History run not found: " + analysisId));
        Project project = history.getProject();
        File workspaceDir = null;

        try {
            if (isCancelled(analysisId)) {
                handleCancellation(history);
                return CompletableFuture.completedFuture(null);
            }

            // Stage 1: Validating
            publishProgress(analysisId, "VALIDATING", 10, "Validating Zip Archive Structures", null);
            updateHistory(history, "VALIDATING", "Validating Project");

            if (isCancelled(analysisId)) {
                handleCancellation(history);
                return CompletableFuture.completedFuture(null);
            }

            // Stage 2: Extracting
            publishProgress(analysisId, "EXTRACTING", 25, "Extracting files to isolated workspace", null);
            updateHistory(history, "EXTRACTING", "Extracting Project");
            
            workspaceDir = workspaceManager.createWorkspace();
            
            // Unzip securely
            fileExtractionService.extractProjectSecurely(zipFile, workspaceDir);
            File projectRoot = fileExtractionService.locateProjectRoot(workspaceDir);

            // Run core pipeline
            runCommonPipeline(analysisId, history, project, projectRoot);

        } catch (Exception e) {
            if (isCancelled(analysisId)) {
                handleCancellation(history);
            } else {
                handlePipelineFailure(history, e);
            }
        } finally {
            // Stage 9: Cleanup zip archive
            if (zipFile != null && zipFile.exists()) {
                fileExtractionService.cleanupFile(zipFile);
            }
            // Cleanup isolated workspace folder
            if (workspaceDir != null) {
                workspaceManager.cleanWorkspace(workspaceDir);
            }
            progressRegistryService.complete(analysisId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("analysisTaskExecutor")
    public CompletableFuture<Void> executeGitPipeline(String analysisId, String repoUrl, String token) {
        ReviewHistory history = reviewHistoryRepository.findByAnalysisId(analysisId)
                .orElseThrow(() -> new RuntimeException("History run not found: " + analysisId));
        Project project = history.getProject();
        File workspaceDir = null;

        try {
            if (isCancelled(analysisId)) {
                handleCancellation(history);
                return CompletableFuture.completedFuture(null);
            }

            // Stage 1: Validating URL
            publishProgress(analysisId, "VALIDATING", 10, "Validating Git URL syntax", null);
            updateHistory(history, "VALIDATING", "Validating URL");
            gitCloningService.validateGitUrl(repoUrl);

            if (isCancelled(analysisId)) {
                handleCancellation(history);
                return CompletableFuture.completedFuture(null);
            }

            // Stage 2: Extracting (Cloning)
            publishProgress(analysisId, "EXTRACTING", 25, "Cloning remote repository structures", null);
            updateHistory(history, "EXTRACTING", "Cloning Repository");
            
            workspaceDir = workspaceManager.createWorkspace();
            gitCloningService.cloneRepositoryToDir(repoUrl, token, workspaceDir);

            // Checkout specific branch/commit/tag if requested
            String refToCheckout = null;
            if (history.getCommitHash() != null && !history.getCommitHash().trim().isEmpty()) {
                refToCheckout = history.getCommitHash();
            } else if (history.getGitTag() != null && !history.getGitTag().trim().isEmpty()) {
                refToCheckout = history.getGitTag();
            } else if (history.getBranch() != null && !history.getBranch().trim().isEmpty()) {
                refToCheckout = history.getBranch();
            }

            if (refToCheckout != null) {
                logger.info("Checking out reference: {}", refToCheckout);
                gitCloningService.checkoutRef(workspaceDir, refToCheckout);
            }

            // Store exact commit hash checked out
            String headCommit = gitCloningService.getHeadCommitHash(workspaceDir);
            if (headCommit != null) {
                history.setCommitHash(headCommit);
            }
            history.setReviewTimestamp(LocalDateTime.now());
            history = reviewHistoryRepository.save(history);

            File projectRoot = fileExtractionService.locateProjectRoot(workspaceDir);

            // Run core pipeline
            runCommonPipeline(analysisId, history, project, projectRoot);

        } catch (Exception e) {
            if (isCancelled(analysisId)) {
                handleCancellation(history);
            } else {
                handlePipelineFailure(history, e);
            }
        } finally {
            // Cleanup isolated workspace folder
            if (workspaceDir != null) {
                workspaceManager.cleanWorkspace(workspaceDir);
            }
            progressRegistryService.complete(analysisId);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void runCommonPipeline(String analysisId, ReviewHistory history, Project project, File projectRoot) throws Exception {
        // Stage 3: Analyzing Project (Structure & Metrics)
        if (isCancelled(analysisId)) throw new InterruptedException("Analysis cancelled");
        publishProgress(analysisId, "ANALYZING", 40, "Scanning Java classes, methods, and configurations", null);
        updateHistory(history, "ANALYZING", "Analyzing Project");

        ProjectAnalysisService.Metrics metrics = null;
        String metricsJson = "{}";
        try {
            metrics = projectAnalysisService.analyzeProject(projectRoot);
            metricsJson = projectAnalysisService.toJson(metrics);
        } catch (Exception e) {
            logger.error("Project structure analysis failed for run: {}", analysisId, e);
            metrics = new ProjectAnalysisService.Metrics(); // Fallback empty metrics to prevent aborting
        }

        // Run extended structural analyses
        String depJson = "{}";
        String configJson = "{}";
        String apiJson = "{}";
        String dbJson = "{}";
        try {
            depJson = dependencyAnalysisService.analyzeDependencies(projectRoot);
            configJson = configurationAnalysisService.analyzeConfigurations(projectRoot);
            apiJson = apiDocumentationAnalysisService.analyzeApis(projectRoot);
            dbJson = databaseAnalysisService.analyzeDatabase(projectRoot);
        } catch (Exception e) {
            logger.error("Advanced structural analysis scans failed", e);
        }

        // Save AnalysisResult initial data
        AnalysisResult analysisResult = AnalysisResult.builder()
                .project(project)
                .language(metrics.language)
                .buildTool(metrics.buildTool)
                .framework(metrics.frameworks.isEmpty() ? "None" : String.join(", ", metrics.frameworks))
                .numPackages(metrics.packagesCount)
                .numClasses(metrics.classesCount)
                .numInterfaces(metrics.interfacesCount)
                .numEnums(metrics.enumsCount)
                .numRecords(metrics.recordsCount)
                .numMethods(metrics.methodsCount)
                .numConstructors(metrics.constructorsCount)
                .numFields(metrics.fieldsCount)
                .scannedFilesJson(new org.json.JSONArray(metrics.scannedFiles).toString())
                .dependencyAnalysisJson(depJson)
                .configurationAnalysisJson(configJson)
                .apiAnalysisJson(apiJson)
                .databaseAnalysisJson(dbJson)
                .build();

        // Stage 4: Running Static Analysis
        if (isCancelled(analysisId)) throw new InterruptedException("Analysis cancelled");
        publishProgress(analysisId, "STATIC_ANALYSIS", 60, "Performing PMD, Checkstyle, and JavaParser scans", null);
        updateHistory(history, "STATIC_ANALYSIS", "Running Static Analysis");

        List<StaticCodeAnalysisService.Finding> findings = null;
        String findingsJson = "[]";
        try {
            findings = staticCodeAnalysisService.performStaticAnalysis(projectRoot, (file) -> {
                // Live file progress update
                publishProgress(analysisId, "STATIC_ANALYSIS", 65, "Scanning code files", file);
            });
            findingsJson = staticCodeAnalysisService.toJson(findings);
        } catch (Exception e) {
            logger.error("Static analysis checks failed for run: {}", analysisId, e);
            findings = new java.util.ArrayList<>();
        }
        analysisResult.setStaticAnalysisIssuesJson(findingsJson);
        analysisResult = analysisResultRepository.save(analysisResult);

        // Stage 5: Executing Unit Tests
        if (isCancelled(analysisId)) throw new InterruptedException("Analysis cancelled");
        publishProgress(analysisId, "RUNNING_TESTS", 80, "Executing Surefire Maven test suites (mvnd)", null);
        updateHistory(history, "RUNNING_TESTS", "Executing Unit Tests");

        UnitTestExecutionService.TestRunResult testRun = null;
        String testRunJson = "{}";
        try {
            testRun = unitTestExecutionService.executeTests(projectRoot);
            testRunJson = unitTestExecutionService.toJson(testRun);
        } catch (Exception e) {
            logger.error("Maven test suite execution failed for run: {}", analysisId, e);
            testRun = new UnitTestExecutionService.TestRunResult(); // empty fallback
        }

        // Save UnitTestResult
        UnitTestResult testResult = UnitTestResult.builder()
                .project(project)
                .totalTests(testRun.totalTests)
                .passed(testRun.passed)
                .failed(testRun.failed)
                .skipped(testRun.skipped)
                .executionTime(testRun.executionTime)
                .failureDetailsJson(testRunJson)
                .build();
        testResult = unitTestResultRepository.save(testResult);

        // Stage 6: Generating AI Review
        if (isCancelled(analysisId)) throw new InterruptedException("Analysis cancelled");
        publishProgress(analysisId, "GENERATING_AI_REVIEW", 90, "Invoking configured AI model review context", null);
        updateHistory(history, "GENERATING_AI_REVIEW", "Generating AI Review");

        String aiReviewResponse = "{}";
        try {
            aiReviewResponse = aiReviewService.generateReviewReport(project, metricsJson, findingsJson, testRunJson);
        } catch (Exception e) {
            logger.error("AI review report context generation failed for run: {}", analysisId, e);
            aiReviewResponse = "{\"error\": \"AI review failed: " + e.getMessage() + "\"}";
        }

        // Stage 7: Creating Final Report
        if (isCancelled(analysisId)) throw new InterruptedException("Analysis cancelled");
        publishProgress(analysisId, "GENERATING_REPORT", 95, "Compiling metrics and exporting PDF", null);
        updateHistory(history, "GENERATING_REPORT", "Creating Final Report");

        Report report = null;
        try {
            report = reportGenerationService.compileAndSaveReport(project, analysisResult, testResult, aiReviewResponse);
        } catch (Exception e) {
            logger.error("Report PDF compilation failed for run: {}", analysisId, e);
            throw e;
        }

        // Stage 8: Completed
        history.setAnalysisResult(analysisResult);
        history.setUnitTestResult(testResult);
        history.setReport(report);
        history.setStatus("COMPLETED");
        history.setStage("Completed");
        history.setCompletedAt(LocalDateTime.now());
        reviewHistoryRepository.save(history);
        
        // Log Audit success
        auditLogService.log(
                project.getUser().getUsername(),
                "Analysis Completion",
                "SUCCESS",
                "Successfully completed analysis run: " + analysisId
        );
        logger.info("Project processing pipeline successfully completed for project: {}", project.getName());
    }

    private boolean isCancelled(String analysisId) {
        ReviewHistory history = reviewHistoryRepository.findByAnalysisId(analysisId).orElse(null);
        return history != null && "CANCELLED".equalsIgnoreCase(history.getStatus());
    }

    private void handleCancellation(ReviewHistory history) {
        logger.info("Pipeline execution cancelled for run Analysis ID: {}", history.getAnalysisId());
        history.setStatus("CANCELLED");
        history.setStage("Cancelled");
        history.setCompletedAt(LocalDateTime.now());
        reviewHistoryRepository.save(history);

        auditLogService.log(
                history.getProject().getUser().getUsername(),
                "Analysis Cancel",
                "SUCCESS",
                "Cancelled review run Analysis ID: " + history.getAnalysisId()
        );
    }

    private void updateHistory(ReviewHistory history, String status, String stage) {
        history.setStatus(status);
        history.setStage(stage);
        reviewHistoryRepository.save(history);
        logger.info("Pipeline Status changed to: {} - Stage: {}", status, stage);
    }

    private void handlePipelineFailure(ReviewHistory history, Exception e) {
        logger.error("Project Processing Pipeline failed for history ID: {}", history.getId(), e);
        history.setStatus("FAILED");
        history.setStage("Failed");
        history.setErrorMessage(e.getMessage() != null ? e.getMessage() : "An unexpected execution error occurred");
        history.setCompletedAt(LocalDateTime.now());
        reviewHistoryRepository.save(history);

        auditLogService.log(
                history.getProject().getUser().getUsername(),
                "Analysis Failure",
                "FAILED",
                "Pipeline run failed. Error: " + history.getErrorMessage()
        );
    }

    private void publishProgress(String analysisId, String status, int percent, String operation, String file) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("status", status);
        progress.put("percentage", percent);
        progress.put("operation", operation);
        progress.put("file", file);
        
        long remaining = Math.round((100 - percent) * 0.4);
        progress.put("remainingSeconds", Math.max(1, remaining));
        
        progressRegistryService.publish(analysisId, progress);
    }
}
