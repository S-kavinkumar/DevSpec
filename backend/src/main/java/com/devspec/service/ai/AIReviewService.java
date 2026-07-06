package com.devspec.service.ai;

import com.devspec.model.AiRequestCache;
import com.devspec.model.AiUsageLog;
import com.devspec.model.Project;
import com.devspec.repository.AiRequestCacheRepository;
import com.devspec.repository.AiUsageLogRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

@Service
public class AIReviewService {
    private static final Logger logger = LoggerFactory.getLogger(AIReviewService.class);

    private final Map<String, AIService> aiServices;
    private final String activeProvider;
    private final AiRequestCacheRepository cacheRepository;
    private final AiUsageLogRepository usageLogRepository;

    public AIReviewService(
            Map<String, AIService> aiServices, 
            @Value("${ai.provider:gemini}") String activeProvider,
            AiRequestCacheRepository cacheRepository,
            AiUsageLogRepository usageLogRepository) {
        this.aiServices = aiServices;
        this.activeProvider = activeProvider.toLowerCase();
        this.cacheRepository = cacheRepository;
        this.usageLogRepository = usageLogRepository;
    }

    public String generateReviewReport(Project project, String projectAnalysisJson, String staticAnalysisJson, String unitTestJson) {
        AIService aiService = aiServices.get(activeProvider);
        if (aiService == null) {
            logger.error("AI provider '{}' is not supported. Supported values: gemini, groq.", activeProvider);
            throw new IllegalArgumentException("Unsupported AI provider: " + activeProvider);
        }

        // Optimize/Compress staticAnalysisJson if length is too large to save tokens
        String optimizedStaticAnalysisJson = optimizeStaticAnalysisJson(staticAnalysisJson);

        String prompt = buildPrompt(projectAnalysisJson, optimizedStaticAnalysisJson, unitTestJson);
        String promptHash = computeSha256(prompt);

        // 1. Cache check
        Optional<AiRequestCache> cached = cacheRepository.findById(promptHash);
        if (cached.isPresent()) {
            logger.info("AI Cache Hit for prompt hash {}. Reusing cached response.", promptHash);
            
            // Log cached usage
            AiUsageLog usageLog = AiUsageLog.builder()
                    .provider(activeProvider)
                    .tokensUsed(0)
                    .requestTimeMs(0L)
                    .costEstimate(0.0)
                    .status("CACHED")
                    .username(project.getUser().getUsername())
                    .operation("AI Review")
                    .build();
            usageLogRepository.save(usageLog);

            return cached.get().getResponseText();
        }

        // 2. Cache Miss - Execute LLM call
        long startTime = System.currentTimeMillis();
        String response;
        String status = "SUCCESS";
        try {
            logger.info("Requesting review from active AI provider: {}", activeProvider);
            response = aiService.generateReview(prompt);
        } catch (Exception e) {
            logger.error("AI Review Generation failed: {}", e.getMessage());
            status = "FAILED";
            response = createFailedFallbackReview(e.getMessage());
        }
        long duration = System.currentTimeMillis() - startTime;

        // Estimate token parameters
        int promptTokens = prompt.length() / 4;
        int responseTokens = response.length() / 4;
        int totalTokens = promptTokens + responseTokens;

        // Estimate query cost
        double cost = estimateCost(activeProvider, promptTokens, responseTokens);

        // Log AI provider execution stats
        AiUsageLog usageLog = AiUsageLog.builder()
                .provider(activeProvider)
                .tokensUsed(totalTokens)
                .requestTimeMs(duration)
                .costEstimate(cost)
                .status(status)
                .username(project.getUser().getUsername())
                .operation("AI Review")
                .build();
        usageLogRepository.save(usageLog);

        // Cache successful executions
        if ("SUCCESS".equals(status)) {
            AiRequestCache cacheEntry = AiRequestCache.builder()
                    .promptHash(promptHash)
                    .responseText(response)
                    .build();
            cacheRepository.save(cacheEntry);
        }

        return response;
    }

    private String optimizeStaticAnalysisJson(String jsonStr) {
        if (jsonStr == null || jsonStr.length() < 15000) {
            return jsonStr;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(jsonStr);
            if (array.length() <= 25) {
                return jsonStr;
            }
            
            // Filter and keep top 25 high-severity findings
            org.json.JSONArray optimizedArray = new org.json.JSONArray();
            int criticalCount = 0;
            int warningCount = 0;
            int otherCount = 0;

            for (int i = 0; i < array.length(); i++) {
                JSONObject finding = array.getJSONObject(i);
                String severity = finding.optString("severity", "Suggestion");
                if ("Critical".equalsIgnoreCase(severity) || "Warning".equalsIgnoreCase(severity)) {
                    if (optimizedArray.length() < 25) {
                        optimizedArray.put(finding);
                    } else {
                        if ("Critical".equalsIgnoreCase(severity)) criticalCount++;
                        else warningCount++;
                    }
                } else {
                    otherCount++;
                }
            }

            // Fill up remaining capacity with suggestions if available
            for (int i = 0; i < array.length() && optimizedArray.length() < 25; i++) {
                JSONObject finding = array.getJSONObject(i);
                String severity = finding.optString("severity", "Suggestion");
                if (!"Critical".equalsIgnoreCase(severity) && !"Warning".equalsIgnoreCase(severity)) {
                    optimizedArray.put(finding);
                }
            }

            JSONObject wrapper = new JSONObject();
            wrapper.put("findings", optimizedArray);
            wrapper.put("omittedCriticalCount", criticalCount);
            wrapper.put("omittedWarningCount", warningCount);
            wrapper.put("omittedOtherCount", otherCount + (array.length() - optimizedArray.length() - criticalCount - warningCount));
            wrapper.put("note", "Omitted low priority static findings to stay within model prompt limits.");

            return wrapper.toString();
        } catch (Exception e) {
            logger.warn("Could not optimize static analysis findings length: {}", e.getMessage());
            return jsonStr;
        }
    }

    private double estimateCost(String provider, int promptTokens, int responseTokens) {
        if ("gemini".equalsIgnoreCase(provider)) {
            // Gemini 1.5 Flash rates: $0.15 / 1M prompt, $0.60 / 1M output
            double inputCost = (promptTokens / 1000000.0) * 0.15;
            double outputCost = (responseTokens / 1000000.0) * 0.60;
            return inputCost + outputCost;
        } else if ("groq".equalsIgnoreCase(provider)) {
            // Groq LLaMA rates: $0.05 / 1M prompt, $0.08 / 1M output
            double inputCost = (promptTokens / 1000000.0) * 0.05;
            double outputCost = (responseTokens / 1000000.0) * 0.08;
            return inputCost + outputCost;
        }
        return ((promptTokens + responseTokens) / 1000000.0) * 0.10;
    }

    private String computeSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 provider missing", e);
        }
    }

    private String buildPrompt(String projectAnalysisJson, String staticAnalysisJson, String unitTestJson) {
        return "You are DEVSPEC, an advanced AI software quality reviewer. Perform a comprehensive quality review of the following software project based on its parsed metrics, static analysis, and test run results. Do NOT attempt to guess raw source code details that are not provided.\n\n" +
                "INPUT METRICS AND STRUCTURAL ANALYSIS (JSON):\n" +
                projectAnalysisJson + "\n\n" +
                "STATIC CODE ANALYSIS FINDINGS (JSON):\n" +
                staticAnalysisJson + "\n\n" +
                "UNIT TEST RUN RESULTS (JSON):\n" +
                unitTestJson + "\n\n" +
                "=========================================\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. NEVER rewrite the user's code. Do not output code replacements or patches.\n" +
                "2. Only provide suggestions that produce meaningful improvements. If the code is already of high quality, explicitly output:\n" +
                "   \"No improvements are recommended because the implementation already follows good software engineering practices.\"\n" +
                "3. Categorize findings into severities: Critical, Warning, Suggestion, Good Practice.\n" +
                "4. Categorize findings into categories: Architecture, Code Quality, Security, Performance, Documentation, Testing, Maintainability, Naming Convention, Dependency Issues.\n" +
                "5. You must output a valid JSON object matching the JSON template below. Do not wrap it in markdown tags or add any text outside the JSON.\n\n" +
                "OUTPUT JSON TEMPLATE:\n" +
                "{\n" +
                "  \"overallScore\": 85.0,\n" +
                "  \"architectureScore\": 80.0,\n" +
                "  \"codeQualityScore\": 90.0,\n" +
                "  \"securityScore\": 85.0,\n" +
                "  \"maintainabilityScore\": 85.0,\n" +
                "  \"documentationScore\": 80.0,\n" +
                "  \"testingScore\": 85.0,\n" +
                "  \"performanceScore\": 90.0,\n" +
                "  \"executiveSummary\": \"Executive Summary of the overall quality analysis.\",\n" +
                "  \"architectureSummary\": \"Architecture Review: A short description of the overall layout, layering, and organization.\",\n" +
                "  \"techStack\": \"List of detected programming languages, libraries, and frameworks.\",\n" +
                "  \"securityAnalysis\": \"Security Review: An evaluation of potential vulnerabilities, leakage points, or secret disclosures.\",\n" +
                "  \"codeQualityReview\": \"Code Quality Review detailing standard formatting, warnings, and code metrics.\",\n" +
                "  \"testingReview\": \"Testing Review explaining unit tests execution summary and gaps.\",\n" +
                "  \"documentationReview\": \"Documentation Review highlighting packages, structures, and comments.\",\n" +
                "  \"maintainabilityReview\": \"Maintainability Review covering cyclomatic complexity and code structure metrics.\",\n" +
                "  \"riskAssessment\": \"Detail risk assessment (Low / Medium / High / Critical).\",\n" +
                "  \"estimatedMaintainability\": \"Detail estimated maintainability (Excellent / Good / Average / Needs Improvement / Critical).\",\n" +
                "  \"estimatedTechnicalDebt\": \"Detail estimated technical debt (Low / Medium / High / Critical).\",\n" +
                "  \"strengths\": [\n" +
                "    \"Description of strength 1\",\n" +
                "    \"Description of strength 2\"\n" +
                "  ],\n" +
                "  \"weaknesses\": [\n" +
                "    \"Description of weakness 1\",\n" +
                "    \"Description of weakness 2\"\n" +
                "  ],\n" +
                "  \"findings\": [\n" +
                "    {\n" +
                "      \"title\": \"Finding summary\",\n" +
                "      \"description\": \"Detailed explanation of what the issue is and why it's a concern.\",\n" +
                "      \"severity\": \"Critical / Warning / Suggestion / Good Practice\",\n" +
                "      \"confidencePercentage\": 95,\n" +
                "      \"category\": \"Architecture / Code Quality / Security / Performance / Documentation / Testing / Maintainability / Naming Convention / Dependency Issues\",\n" +
                "      \"reason\": \"Specific reason why this was flagged.\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"finalVerdict\": \"Overall summary judgment of the project quality.\"\n" +
                "}";
    }

    private String createFailedFallbackReview(String errorMsg) {
        JSONObject fallback = new JSONObject();
        fallback.put("overallScore", 50.0);
        fallback.put("architectureScore", 50.0);
        fallback.put("codeQualityScore", 50.0);
        fallback.put("securityScore", 50.0);
        fallback.put("maintainabilityScore", 50.0);
        fallback.put("documentationScore", 50.0);
        fallback.put("testingScore", 50.0);
        fallback.put("performanceScore", 50.0);
        
        fallback.put("executiveSummary", "Review aborted because the configured AI provider encountered a processing issue.");
        fallback.put("architectureSummary", "Standard architecture outline.");
        fallback.put("techStack", "Unknown / Undetermined");
        fallback.put("securityAnalysis", "Unable to run AI security evaluation. Reason: " + errorMsg);
        
        org.json.JSONArray strengths = new org.json.JSONArray();
        strengths.put("None captured (AI Review Error).");
        fallback.put("strengths", strengths);

        org.json.JSONArray weaknesses = new org.json.JSONArray();
        weaknesses.put("AI Service integration failure: " + errorMsg);
        fallback.put("weaknesses", weaknesses);

        org.json.JSONArray findings = new org.json.JSONArray();
        JSONObject errorFinding = new JSONObject();
        errorFinding.put("title", "AI Review Service Interrupted");
        errorFinding.put("description", "The AI review step failed to complete because of an integration error: " + errorMsg);
        errorFinding.put("severity", "Critical");
        errorFinding.put("confidencePercentage", 100);
        errorFinding.put("category", "Security");
        errorFinding.put("reason", "API service timed out or authentication was denied.");
        findings.put(errorFinding);
        fallback.put("findings", findings);

        fallback.put("finalVerdict", "Please verify your AI API key and connection configuration.");
        return fallback.toString();
    }
}
