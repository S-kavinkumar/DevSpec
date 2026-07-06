package com.devspec.service.report;

import com.devspec.model.Report;
import com.devspec.service.ai.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportComparisonService {
    private final Map<String, AIService> aiServices;
    private final String activeProvider;

    public ReportComparisonService(
            Map<String, AIService> aiServices, 
            @Value("${ai.provider:gemini}") String activeProvider) {
        this.aiServices = aiServices;
        this.activeProvider = activeProvider.toLowerCase();
    }

    public Map<String, Object> compareReports(Report report1, Report report2) {
        Map<String, Object> comparison = new HashMap<>();
        
        comparison.put("report1Id", report1.getId());
        comparison.put("report1Version", report1.getReportVersion());
        comparison.put("report1Date", report1.getCreatedAt());
        
        comparison.put("report2Id", report2.getId());
        comparison.put("report2Version", report2.getReportVersion());
        comparison.put("report2Date", report2.getCreatedAt());

        comparison.put("scores", compareScoreFields(report1, report2));

        // Generate AI comparison summary
        String aiSummary = generateAIComparisonSummary(report1, report2);
        comparison.put("aiSummary", aiSummary);

        return comparison;
    }

    private Map<String, Map<String, Object>> compareScoreFields(Report r1, Report r2) {
        Map<String, Map<String, Object>> scoreDiffs = new HashMap<>();
        
        scoreDiffs.put("overallScore", getDiff(r1.getOverallScore(), r2.getOverallScore()));
        scoreDiffs.put("architectureScore", getDiff(r1.getArchitectureScore(), r2.getArchitectureScore()));
        scoreDiffs.put("codeQualityScore", getDiff(r1.getCodeQualityScore(), r2.getCodeQualityScore()));
        scoreDiffs.put("securityScore", getDiff(r1.getSecurityScore(), r2.getSecurityScore()));
        scoreDiffs.put("testingScore", getDiff(r1.getTestingScore(), r2.getTestingScore()));
        scoreDiffs.put("documentationScore", getDiff(r1.getDocumentationScore(), r2.getDocumentationScore()));
        scoreDiffs.put("maintainabilityScore", getDiff(r1.getMaintainabilityScore(), r2.getMaintainabilityScore()));
        scoreDiffs.put("performanceScore", getDiff(r1.getPerformanceScore(), r2.getPerformanceScore()));

        return scoreDiffs;
    }

    private Map<String, Object> getDiff(double val1, double val2) {
        Map<String, Object> diff = new HashMap<>();
        diff.put("val1", val1);
        diff.put("val2", val2);
        diff.put("difference", Math.round((val2 - val1) * 10.0) / 10.0);
        
        if (val2 > val1) {
            diff.put("status", "Improved");
        } else if (val2 < val1) {
            diff.put("status", "Declined");
        } else {
            diff.put("status", "Unchanged");
        }
        return diff;
    }

    private String generateAIComparisonSummary(Report r1, Report r2) {
        AIService aiService = aiServices.get(activeProvider);
        if (aiService == null) {
            return "AI comparison summary unavailable. Configured AI provider details are missing.";
        }

        String prompt = "You are DEVSPEC, an expert software architecture and code quality analyst. " +
                "Analyze and compare these two reports for the project '" + r1.getProject().getName() + "'. " +
                "Compare the scores and summarize the progress, changes, regressions, or improvements.\n\n" +
                "REPORT 1 (v" + r1.getReportVersion() + " | Date: " + r1.getCreatedAt() + "):\n" +
                "- Overall Score: " + r1.getOverallScore() + "\n" +
                "- Architecture Score: " + r1.getArchitectureScore() + "\n" +
                "- Code Quality Score: " + r1.getCodeQualityScore() + "\n" +
                "- Security Score: " + r1.getSecurityScore() + "\n" +
                "- Testing Score: " + r1.getTestingScore() + "\n" +
                "- Maintainability Score: " + r1.getMaintainabilityScore() + "\n" +
                "- Documentation Score: " + r1.getDocumentationScore() + "\n" +
                "- Performance Score: " + r1.getPerformanceScore() + "\n\n" +
                "REPORT 2 (v" + r2.getReportVersion() + " | Date: " + r2.getCreatedAt() + "):\n" +
                "- Overall Score: " + r2.getOverallScore() + "\n" +
                "- Architecture Score: " + r2.getArchitectureScore() + "\n" +
                "- Code Quality Score: " + r2.getCodeQualityScore() + "\n" +
                "- Security Score: " + r2.getSecurityScore() + "\n" +
                "- Testing Score: " + r2.getTestingScore() + "\n" +
                "- Maintainability Score: " + r2.getMaintainabilityScore() + "\n" +
                "- Documentation Score: " + r2.getDocumentationScore() + "\n" +
                "- Performance Score: " + r2.getPerformanceScore() + "\n\n" +
                "Write a professional, concise summary comparison (approx. 2-3 paragraphs) explaining what has improved or declined between these runs and providing constructive architectural feedback.";

        try {
            return aiService.generateReview(prompt);
        } catch (Exception e) {
            return "Error invoking AI comparison generator: " + e.getMessage();
        }
    }
}
