package com.devspec.controller;

import com.devspec.exception.BadRequestException;
import com.devspec.exception.ResourceNotFoundException;
import com.devspec.model.*;
import com.devspec.repository.*;
import com.devspec.service.report.ReportComparisonService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReportRepository reportRepository;
    private final ReviewHistoryRepository reviewHistoryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ReportComparisonService comparisonService;

    public ReportController(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ReportRepository reportRepository,
            ReviewHistoryRepository reviewHistoryRepository,
            AnalysisResultRepository analysisResultRepository,
            ReportComparisonService comparisonService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.reportRepository = reportRepository;
        this.reviewHistoryRepository = reviewHistoryRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.comparisonService = comparisonService;
    }

    // Node representation for hierarchical folder structure
    public static class FileNode {
        public String name;
        public String path;
        public String type; // file, dir
        public List<FileNode> children = new ArrayList<>();

        public FileNode(String name, String path, String type) {
            this.name = name;
            this.path = path;
            this.type = type;
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        User user = getCurrentUser(authentication);
        logger.info("Fetching dashboard metrics for user: {}", user.getUsername());

        List<Project> projects = projectRepository.findByUserId(user.getId());
        List<ReviewHistory> histories = reviewHistoryRepository.findByProjectUserIdOrderByCreatedAtDesc(user.getId());

        long totalProjects = projects.size();
        
        // Calculate Reviewed Projects (unique project IDs with COMPLETED status)
        Set<Long> reviewedProjectIds = new HashSet<>();
        List<Report> allCompletedReports = new ArrayList<>();
        double sumOverall = 0.0;
        double sumSecurity = 0.0;
        double sumTestPassRate = 0.0;
        int completedCount = 0;
        int reportsWithTestsCount = 0;

        // Collect all completed reports for calculations
        for (ReviewHistory rh : histories) {
            if ("COMPLETED".equals(rh.getStatus()) && rh.getReport() != null) {
                reviewedProjectIds.add(rh.getProject().getId());
                Report r = rh.getReport();
                allCompletedReports.add(r);
                
                sumOverall += r.getOverallScore();
                sumSecurity += r.getSecurityScore();
                completedCount++;

                if (rh.getUnitTestResult() != null && rh.getUnitTestResult().getTotalTests() > 0) {
                    UnitTestResult ut = rh.getUnitTestResult();
                    double passRate = (ut.getPassed() * 100.0) / ut.getTotalTests();
                    sumTestPassRate += passRate;
                    reportsWithTestsCount++;
                }
            }
        }

        double avgProjectScore = completedCount > 0 ? (sumOverall / completedCount) : 0.0;
        double avgSecurityScore = completedCount > 0 ? (sumSecurity / completedCount) : 0.0;
        double avgTestPassRate = reportsWithTestsCount > 0 ? (sumTestPassRate / reportsWithTestsCount) : 0.0;

        // Chart 1: Issue Severity Distribution (Critical, Warning, Suggestion/Good Practice)
        int totalCritical = 0;
        int totalWarning = 0;
        int totalSuggestions = 0;
        Map<String, Integer> issueCounts = new HashMap<>();

        // Chart 2: Technology Stack distribution
        Map<String, Integer> techCounts = new HashMap<>();

        for (ReviewHistory rh : histories) {
            if ("COMPLETED".equals(rh.getStatus()) && rh.getAnalysisResult() != null) {
                AnalysisResult ar = rh.getAnalysisResult();
                
                // Count severity frequencies from JSON
                try {
                    JSONArray arr = new JSONArray(ar.getStaticAnalysisIssuesJson());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject finding = arr.getJSONObject(i);
                        String severity = finding.optString("severity", "Suggestion");
                        String title = finding.optString("title", "Unknown Code Issue");
                        
                        issueCounts.put(title, issueCounts.getOrDefault(title, 0) + 1);

                        if ("Critical".equalsIgnoreCase(severity)) totalCritical++;
                        else if ("Warning".equalsIgnoreCase(severity)) totalWarning++;
                        else totalSuggestions++;
                    }
                } catch (Exception e) {
                    // Ignore parsing anomalies
                }

                // Tech mappings
                String lang = ar.getLanguage() != null ? ar.getLanguage() : "Java";
                techCounts.put(lang, techCounts.getOrDefault(lang, 0) + 1);
                
                if (ar.getFramework() != null && !ar.getFramework().equalsIgnoreCase("none")) {
                    for (String fw : ar.getFramework().split(",")) {
                        String cleanFw = fw.trim();
                        if (!cleanFw.isEmpty()) {
                            techCounts.put(cleanFw, techCounts.getOrDefault(cleanFw, 0) + 1);
                        }
                    }
                }
            }
        }

        // Score Distribution Charts (80-100, 60-79, <60 ranges)
        int rangeHigh = 0; // 80-100
        int rangeMid = 0;  // 60-79
        int rangeLow = 0;  // <60
        for (Report r : allCompletedReports) {
            double score = r.getOverallScore();
            if (score >= 80) rangeHigh++;
            else if (score >= 60) rangeMid++;
            else rangeLow++;
        }

        // Recent Reports listing DTOs
        List<Map<String, Object>> recentReports = new ArrayList<>();
        int count = 0;
        for (Report r : allCompletedReports) {
            if (count >= 5) break;
            Map<String, Object> map = new HashMap<>();
            map.put("reportId", r.getId());
            map.put("projectId", r.getProject().getId());
            map.put("projectName", r.getProject().getName());
            map.put("overallScore", r.getOverallScore());
            map.put("version", r.getReportVersion());
            map.put("date", r.getCreatedAt());
            recentReports.add(map);
            count++;
        }

        // Recent AI Suggestions
        List<Map<String, Object>> recentSuggestions = new ArrayList<>();
        int suggCount = 0;
        for (Report r : allCompletedReports) {
            if (suggCount >= 5) break;
            try {
                JSONArray arr = new JSONArray(r.getAiSuggestionsJson());
                for (int i = 0; i < arr.length() && suggCount < 5; i++) {
                    JSONObject sug = arr.getJSONObject(i);
                    Map<String, Object> map = new HashMap<>();
                    map.put("projectName", r.getProject().getName());
                    map.put("title", sug.optString("title"));
                    map.put("severity", sug.optString("severity"));
                    map.put("description", sug.optString("description"));
                    recentSuggestions.add(map);
                    suggCount++;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Top 5 Most Common Issues
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(issueCounts.entrySet());
        entryList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        List<Map<String, Object>> topCommonIssues = new ArrayList<>();
        for (int i = 0; i < Math.min(5, entryList.size()); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", entryList.get(i).getKey());
            item.put("count", entryList.get(i).getValue());
            topCommonIssues.add(item);
        }

        // Calculate average analysis duration
        long totalDurationMs = 0;
        int durationCount = 0;
        for (ReviewHistory rh : histories) {
            if ("COMPLETED".equals(rh.getStatus()) && rh.getCompletedAt() != null && rh.getCreatedAt() != null) {
                long duration = java.time.Duration.between(rh.getCreatedAt(), rh.getCompletedAt()).toMillis();
                totalDurationMs += duration;
                durationCount++;
            }
        }
        double avgDurationSeconds = durationCount > 0 ? (totalDurationMs / 1000.0 / durationCount) : 0.0;
        double averageAnalysisDuration = Math.round(avgDurationSeconds * 10.0) / 10.0;

        // Project Health Cards distribution
        Map<String, Integer> healthCounts = new HashMap<>();
        healthCounts.put("Excellent", 0);
        healthCounts.put("Good", 0);
        healthCounts.put("Average", 0);
        healthCounts.put("Needs Improvement", 0);
        healthCounts.put("Critical", 0);

        for (Report r : allCompletedReports) {
            double score = r.getOverallScore();
            if (score >= 90) healthCounts.put("Excellent", healthCounts.get("Excellent") + 1);
            else if (score >= 80) healthCounts.put("Good", healthCounts.get("Good") + 1);
            else if (score >= 70) healthCounts.put("Average", healthCounts.get("Average") + 1);
            else if (score >= 50) healthCounts.put("Needs Improvement", healthCounts.get("Needs Improvement") + 1);
            else healthCounts.put("Critical", healthCounts.get("Critical") + 1);
        }

        // Assemble result JSON Map
        Map<String, Object> resultPayload = new HashMap<>();
        resultPayload.put("totalProjects", totalProjects);
        resultPayload.put("projectsReviewed", reviewedProjectIds.size());
        resultPayload.put("averageProjectScore", Math.round(avgProjectScore * 10.0) / 10.0);
        resultPayload.put("averageSecurityScore", Math.round(avgSecurityScore * 10.0) / 10.0);
        resultPayload.put("averageTestPassRate", Math.round(avgTestPassRate * 10.0) / 10.0);
        resultPayload.put("averageAnalysisDuration", averageAnalysisDuration);
        resultPayload.put("healthDistribution", healthCounts);
        
        resultPayload.put("recentReports", recentReports);
        resultPayload.put("recentSuggestions", recentSuggestions);
        resultPayload.put("topCommonIssues", topCommonIssues);

        // Chart Mappings
        resultPayload.put("scoreDistribution", List.of(
                Map.of("name", "80-100", "value", rangeHigh),
                Map.of("name", "60-79", "value", rangeMid),
                Map.of("name", "<60", "value", rangeLow)
        ));

        resultPayload.put("severityDistribution", List.of(
                Map.of("name", "Critical", "value", totalCritical),
                Map.of("name", "Warning", "value", totalWarning),
                Map.of("name", "Suggestion", "value", totalSuggestions)
        ));

        List<Map<String, Object>> techList = new ArrayList<>();
        techCounts.forEach((k, v) -> techList.add(Map.of("name", k, "value", v)));
        resultPayload.put("technologyDistribution", techList);

        // Project Review Trend (historical reviews grouped by date)
        List<Map<String, Object>> trendList = new ArrayList<>();
        int trendLimit = Math.min(10, allCompletedReports.size());
        for (int i = trendLimit - 1; i >= 0; i--) {
            Report r = allCompletedReports.get(i);
            trendList.add(Map.of(
                    "date", r.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd")),
                    "score", r.getOverallScore(),
                    "name", r.getProject().getName()
            ));
        }
        resultPayload.put("projectReviewTrend", trendList);

        return ResponseEntity.ok(resultPayload);
    }

    @GetMapping("/compare")
    public ResponseEntity<?> compareReports(
            @RequestParam("reportId1") Long reportId1,
            @RequestParam("reportId2") Long reportId2,
            Authentication authentication) {
        
        User user = getCurrentUser(authentication);
        logger.info("User '{}' requested comparison between report {} and report {}", user.getUsername(), reportId1, reportId2);

        Report r1 = reportRepository.findById(reportId1)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId1));
        
        Report r2 = reportRepository.findById(reportId2)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId2));

        // Validate ownership
        if (!r1.getProject().getUser().getId().equals(user.getId()) ||
                !r2.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: You do not own these projects.");
        }

        Map<String, Object> diffMap = comparisonService.compareReports(r1, r2);
        return ResponseEntity.ok(diffMap);
    }

    @GetMapping("/{projectId}/history")
    public ResponseEntity<?> getProjectReportHistory(@PathVariable Long projectId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to project report history");
        }

        List<Report> reports = reportRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Report r : reports) {
            Map<String, Object> map = new HashMap<>();
            map.put("reportId", r.getId());
            map.put("reportVersion", r.getReportVersion());
            map.put("projectVersion", r.getProjectVersion());
            map.put("overallScore", r.getOverallScore());
            map.put("reviewDate", r.getCreatedAt());
            map.put("reviewer", r.getReviewer());
            response.add(map);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail/{reportId}")
    public ResponseEntity<?> getReportDetail(@PathVariable Long reportId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        if (!report.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to report details");
        }

        // We fetch matching analysis results and unit test runs
        ReviewHistory historyRun = reviewHistoryRepository.findAll().stream()
                .filter(rh -> rh.getReport() != null && rh.getReport().getId().equals(reportId))
                .findFirst().orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("id", report.getId());
        response.put("projectId", report.getProject().getId());
        response.put("projectName", report.getProject().getName());
        response.put("projectType", report.getProject().getType());
        response.put("tags", report.getProject().getTags());
        response.put("reportVersion", report.getReportVersion());
        response.put("projectVersion", report.getProjectVersion());
        response.put("reviewDate", report.getCreatedAt());
        response.put("reviewer", report.getReviewer());

        // Component scores
        response.put("overallScore", report.getOverallScore());
        response.put("architectureScore", report.getArchitectureScore());
        response.put("codeQualityScore", report.getCodeQualityScore());
        response.put("securityScore", report.getSecurityScore());
        response.put("maintainabilityScore", report.getMaintainabilityScore());
        response.put("documentationScore", report.getDocumentationScore());
        response.put("testingScore", report.getTestingScore());
        response.put("performanceScore", report.getPerformanceScore());

        // Text reviews
        response.put("executiveSummary", report.getExecutiveSummary());
        response.put("architectureSummary", report.getArchitectureSummary());
        response.put("techStack", report.getTechStack());
        response.put("securityAnalysis", report.getSecurityAnalysis());
        response.put("finalVerdict", report.getFinalVerdict());
        
        response.put("strengths", report.getStrengthsJson());
        response.put("weaknesses", report.getWeaknessesJson());
        response.put("aiSuggestions", report.getAiSuggestionsJson());

        // Git metadata
        response.put("repositoryName", report.getRepositoryName());
        response.put("branch", report.getBranch());
        response.put("commitHash", report.getCommitHash());
        response.put("gitTag", report.getGitTag());
        response.put("reviewTimestamp", report.getReviewTimestamp());

        // Dynamic assessments
        response.put("riskAssessment", report.getRiskAssessment());
        response.put("estimatedMaintainability", report.getEstimatedMaintainability());
        response.put("estimatedTechnicalDebt", report.getEstimatedTechnicalDebt());

        // Technical Debt estimates
        response.put("techDebtHours", report.getTechDebtHours());
        response.put("techDebtComplexity", report.getTechDebtComplexity());
        response.put("techDebtRisk", report.getTechDebtRisk());
        response.put("techDebtPriority", report.getTechDebtPriority());
        
        if (report.getReviewInsightsJson() != null) {
            response.put("reviewInsights", new com.fasterxml.jackson.databind.util.RawValue(report.getReviewInsightsJson()));
        }

        if (historyRun != null) {
            if (historyRun.getAnalysisResult() != null) {
                AnalysisResult ar = historyRun.getAnalysisResult();
                response.put("analysisMetrics", new com.fasterxml.jackson.databind.util.RawValue(ar.getStaticAnalysisIssuesJson()));
                
                Map<String, Object> struct = new HashMap<>();
                struct.put("language", ar.getLanguage());
                struct.put("buildTool", ar.getBuildTool());
                struct.put("framework", ar.getFramework());
                struct.put("numPackages", ar.getNumPackages());
                struct.put("numClasses", ar.getNumClasses());
                struct.put("numInterfaces", ar.getNumInterfaces());
                struct.put("numEnums", ar.getNumEnums());
                struct.put("numRecords", ar.getNumRecords());
                struct.put("numMethods", ar.getNumMethods());
                struct.put("numConstructors", ar.getNumConstructors());
                struct.put("numFields", ar.getNumFields());
                response.put("structuralMetrics", struct);

                // Add advanced analyses
                if (ar.getDependencyAnalysisJson() != null) {
                    response.put("dependencyAnalysis", new com.fasterxml.jackson.databind.util.RawValue(ar.getDependencyAnalysisJson()));
                }
                if (ar.getConfigurationAnalysisJson() != null) {
                    response.put("configurationAnalysis", new com.fasterxml.jackson.databind.util.RawValue(ar.getConfigurationAnalysisJson()));
                }
                if (ar.getApiAnalysisJson() != null) {
                    response.put("apiAnalysis", new com.fasterxml.jackson.databind.util.RawValue(ar.getApiAnalysisJson()));
                }
                if (ar.getDatabaseAnalysisJson() != null) {
                    response.put("databaseAnalysis", new com.fasterxml.jackson.databind.util.RawValue(ar.getDatabaseAnalysisJson()));
                }
            }
            if (historyRun.getUnitTestResult() != null) {
                response.put("unitTestResult", new com.fasterxml.jackson.databind.util.RawValue(historyRun.getUnitTestResult().getFailureDetailsJson()));
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/structure")
    public ResponseEntity<?> getProjectStructureTree(@PathVariable Long projectId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to project structure");
        }

        // Fetch latest completed analysis result for relative file list
        AnalysisResult latestAnalysis = analysisResultRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis result not found for project: " + projectId));

        if (latestAnalysis.getScannedFilesJson() == null) {
            return ResponseEntity.ok(new FileNode("root", "", "dir"));
        }

        try {
            JSONArray arr = new JSONArray(latestAnalysis.getScannedFilesJson());
            List<String> paths = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                paths.add(arr.getString(i));
            }
            
            // Build hierarchical tree
            FileNode root = buildPrefixTrieTree(paths);
            return ResponseEntity.ok(root);
        } catch (Exception e) {
            logger.error("Failed to parse scanned files to folder structure tree", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to construct project tree layout.");
        }
    }

    private FileNode buildPrefixTrieTree(List<String> paths) {
        FileNode root = new FileNode("root", "", "dir");
        for (String path : paths) {
            String[] parts = path.split("/");
            FileNode current = root;
            StringBuilder currentPath = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) currentPath.append("/");
                currentPath.append(parts[i]);
                
                String name = parts[i];
                String type = (i == parts.length - 1) ? "file" : "dir";
                
                FileNode child = findChildNode(current, name);
                if (child == null) {
                    child = new FileNode(name, currentPath.toString(), type);
                    current.children.add(child);
                }
                current = child;
            }
        }
        return root;
    }

    private FileNode findChildNode(FileNode parent, String name) {
        for (FileNode child : parent.children) {
            if (child.name.equals(name)) {
                return child;
            }
        }
        return null;
    }

    @GetMapping("/export/json/{reportId}")
    public ResponseEntity<byte[]> exportReportJson(@PathVariable Long reportId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        if (!report.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ReviewHistory historyRun = reviewHistoryRepository.findAll().stream()
                .filter(rh -> rh.getReport() != null && rh.getReport().getId().equals(reportId))
                .findFirst().orElse(null);

        JSONObject json = new JSONObject();
        json.put("reportId", report.getId());
        json.put("projectName", report.getProject().getName());
        json.put("reportVersion", report.getReportVersion());
        json.put("projectVersion", report.getProjectVersion());
        json.put("overallScore", report.getOverallScore());
        json.put("architectureScore", report.getArchitectureScore());
        json.put("codeQualityScore", report.getCodeQualityScore());
        json.put("securityScore", report.getSecurityScore());
        json.put("maintainabilityScore", report.getMaintainabilityScore());
        json.put("documentationScore", report.getDocumentationScore());
        json.put("testingScore", report.getTestingScore());
        json.put("performanceScore", report.getPerformanceScore());
        json.put("executiveSummary", report.getExecutiveSummary());
        json.put("architectureSummary", report.getArchitectureSummary());
        json.put("techStack", report.getTechStack());
        json.put("securityAnalysis", report.getSecurityAnalysis());
        json.put("finalVerdict", report.getFinalVerdict());
        
        json.put("strengths", new JSONArray(report.getStrengthsJson()));
        json.put("weaknesses", new JSONArray(report.getWeaknessesJson()));
        json.put("aiSuggestions", new JSONArray(report.getAiSuggestionsJson()));

        if (historyRun != null && historyRun.getAnalysisResult() != null) {
            AnalysisResult ar = historyRun.getAnalysisResult();
            json.put("language", ar.getLanguage());
            json.put("buildTool", ar.getBuildTool());
            json.put("framework", ar.getFramework());
            json.put("staticAnalysisIssues", new JSONArray(ar.getStaticAnalysisIssuesJson()));
            if (ar.getDependencyAnalysisJson() != null) {
                json.put("dependencyAnalysis", new JSONObject(ar.getDependencyAnalysisJson()));
            }
            if (ar.getConfigurationAnalysisJson() != null) {
                json.put("configurationAnalysis", new JSONObject(ar.getConfigurationAnalysisJson()));
            }
            if (ar.getApiAnalysisJson() != null) {
                json.put("apiAnalysis", new JSONObject(ar.getApiAnalysisJson()));
            }
            if (ar.getDatabaseAnalysisJson() != null) {
                json.put("databaseAnalysis", new JSONObject(ar.getDatabaseAnalysisJson()));
            }
        }

        byte[] bytes = json.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileName = "Report_" + report.getProject().getName() + "_" + report.getReportVersion() + ".json";

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(bytes);
    }

    @GetMapping("/export/html/{reportId}")
    public ResponseEntity<byte[]> exportReportHtml(@PathVariable Long reportId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with ID: " + reportId));

        if (!report.getProject().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ReviewHistory historyRun = reviewHistoryRepository.findAll().stream()
                .filter(rh -> rh.getReport() != null && rh.getReport().getId().equals(reportId))
                .findFirst().orElse(null);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n")
            .append("<title>DEVSPEC Quality Report - ").append(report.getProject().getName()).append("</title>\n")
            .append("<meta charset='utf-8'>\n")
            .append("<style>\n")
            .append("body { font-family: 'Inter', sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 40px; }\n")
            .append(".container { max-width: 1000px; margin: 0 auto; background: rgba(30, 41, 59, 0.7); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.05); padding: 40px; border-radius: 16px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }\n")
            .append("h1, h2, h3 { color: #06b6d4; }\n")
            .append(".score-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin: 30px 0; }\n")
            .append(".score-card { background: rgba(15, 23, 42, 0.6); padding: 20px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.05); text-align: center; }\n")
            .append(".score-num { font-size: 32px; font-weight: bold; color: #10b981; margin-top: 10px; }\n")
            .append(".section { margin-top: 40px; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 20px; }\n")
            .append(".list-item { background: rgba(255,255,255,0.02); padding: 12px; margin: 8px 0; border-radius: 8px; border-left: 4px solid #06b6d4; }\n")
            .append(".finding-card { background: rgba(15, 23, 42, 0.4); border-left: 4px solid #ef4444; padding: 15px; margin: 15px 0; border-radius: 8px; }\n")
            .append(".finding-title { font-weight: bold; font-size: 16px; margin-bottom: 5px; }\n")
            .append(".finding-meta { font-size: 12px; color: #94a3b8; }\n")
            .append("</style>\n</head>\n<body>\n<div class='container'>\n")
            .append("<h1>DEVSPEC Audit Review</h1>\n")
            .append("<p>Project: <strong>").append(report.getProject().getName()).append("</strong> | Report: ").append(report.getReportVersion()).append(" | Date: ").append(report.getCreatedAt()).append("</p>\n")
            .append("<div class='score-grid'>\n")
            .append("<div class='score-card'><div>OVERALL</div><div class='score-num'>").append(String.format("%.1f", report.getOverallScore())).append("</div></div>\n")
            .append("<div class='score-card'><div>ARCHITECTURE</div><div class='score-num'>").append(String.format("%.1f", report.getArchitectureScore())).append("</div></div>\n")
            .append("<div class='score-card'><div>CODE QUALITY</div><div class='score-num'>").append(String.format("%.1f", report.getCodeQualityScore())).append("</div></div>\n")
            .append("<div class='score-card'><div>SECURITY</div><div class='score-num'>").append(String.format("%.1f", report.getSecurityScore())).append("</div></div>\n")
            .append("</div>\n")
            .append("<div class='section'>\n<h2>Executive Summary</h2>\n<p>").append(report.getExecutiveSummary()).append("</p>\n</div>\n")
            .append("<div class='section'>\n<h2>Strengths</h2>\n");

        JSONArray strengths = new JSONArray(report.getStrengthsJson());
        for (int i = 0; i < strengths.length(); i++) {
            html.append("<div class='list-item'>").append(strengths.getString(i)).append("</div>\n");
        }

        html.append("</div>\n<div class='section'>\n<h2>Weaknesses</h2>\n");
        JSONArray weaknesses = new JSONArray(report.getWeaknessesJson());
        for (int i = 0; i < weaknesses.length(); i++) {
            html.append("<div class='list-item'>").append(weaknesses.getString(i)).append("</div>\n");
        }

        html.append("</div>\n<div class='section'>\n<h2>AI Suggestions and Findings</h2>\n");
        JSONArray findings = new JSONArray(report.getAiSuggestionsJson());
        for (int i = 0; i < findings.length(); i++) {
            JSONObject find = findings.getJSONObject(i);
            html.append("<div class='finding-card'>\n")
                .append("<div class='finding-title'>").append(find.optString("title")).append("</div>\n")
                .append("<p>").append(find.optString("description")).append("</p>\n")
                .append("<div class='finding-meta'>Category: ").append(find.optString("category")).append(" | Severity: ").append(find.optString("severity")).append(" | Reason: ").append(find.optString("reason")).append("</div>\n")
                .append("</div>\n");
        }

        html.append("</div>\n<div class='section'>\n<h2>Final Verdict</h2>\n<p>").append(report.getFinalVerdict()).append("</p>\n</div>\n")
            .append("</div>\n</body>\n</html>");

        byte[] bytes = html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileName = "Report_" + report.getProject().getName() + "_" + report.getReportVersion() + ".html";

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(bytes);
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new BadRequestException("User not authenticated");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadRequestException("Authenticated user details not found"));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchReports(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "minScore", required = false) Double minScore,
            @RequestParam(value = "maxScore", required = false) Double maxScore,
            @RequestParam(value = "technology", required = false) String technology,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "username", required = false) String targetUsername,
            @RequestParam(value = "sortBy", defaultValue = "date") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        List<Report> reports = reportRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Report r : reports) {
            Project p = r.getProject();
            if (p == null) continue;

            // RBAC restriction: regular users only see their own projects
            if (!isAdmin && !p.getUser().getId().equals(currentUser.getId())) {
                continue;
            }

            // Filter by targetUsername if Admin
            if (isAdmin && targetUsername != null && !targetUsername.trim().isEmpty()) {
                if (!p.getUser().getUsername().equalsIgnoreCase(targetUsername.trim())) {
                    continue;
                }
            }

            // Filter by search term
            if (search != null && !search.trim().isEmpty()) {
                String term = search.toLowerCase().trim();
                boolean matches = p.getName().toLowerCase().contains(term) ||
                                  (r.getReviewer() != null && r.getReviewer().toLowerCase().contains(term));
                if (!matches) continue;
            }

            // Filter by Score range
            if (minScore != null && r.getOverallScore() < minScore) continue;
            if (maxScore != null && r.getOverallScore() > maxScore) continue;

            // Filter by technology type
            if (technology != null && !technology.trim().isEmpty()) {
                if (!p.getType().equalsIgnoreCase(technology.trim())) {
                    continue;
                }
            }

            // Filter by project tag
            if (tag != null && !tag.trim().isEmpty()) {
                boolean hasTag = p.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(tag.trim()));
                if (!hasTag) continue;
            }

            // Filter by run status
            if (status != null && !status.trim().isEmpty()) {
                if (!"COMPLETED".equalsIgnoreCase(status.trim())) {
                    continue;
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("reportId", r.getId());
            map.put("reportVersion", r.getReportVersion());
            map.put("projectVersion", r.getProjectVersion());
            map.put("projectName", p.getName());
            map.put("projectType", p.getType());
            map.put("overallScore", r.getOverallScore());
            map.put("createdAt", r.getCreatedAt());
            map.put("reviewer", r.getReviewer());
            map.put("username", p.getUser().getUsername());
            map.put("tags", p.getTags());
            results.add(map);
        }

        // Sorting
        Comparator<Map<String, Object>> comp = (a, b) -> {
            if ("score".equalsIgnoreCase(sortBy)) {
                Double sa = (Double) a.get("overallScore");
                Double sb = (Double) b.get("overallScore");
                return sa.compareTo(sb);
            } else {
                LocalDateTime da = (LocalDateTime) a.get("createdAt");
                LocalDateTime db = (LocalDateTime) b.get("createdAt");
                if (da == null || db == null) return 0;
                return da.compareTo(db);
            }
        };

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comp = comp.reversed();
        }

        results.sort(comp);

        return ResponseEntity.ok(results);
    }
}
