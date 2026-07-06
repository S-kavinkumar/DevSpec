package com.devspec.service.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service("gemini")
public class GeminiAIService implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    private final String apiKey;
    private final RestTemplate restTemplate;

    public GeminiAIService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String generateReview(String prompt) throws Exception {
        if (!StringUtils.hasText(apiKey) || apiKey.startsWith("${")) {
            logger.warn("Gemini API key is not configured. Falling back to mock generator.");
            return generateMockReview();
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject requestJson = new JSONObject();
        JSONArray contentsArray = new JSONArray();
        JSONObject contentsObject = new JSONObject();
        JSONArray partsArray = new JSONArray();
        JSONObject partText = new JSONObject();
        partText.put("text", prompt);
        partsArray.put(partText);
        contentsObject.put("parts", partsArray);
        contentsArray.put(contentsObject);
        requestJson.put("contents", contentsArray);

        // Request JSON output
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        requestJson.put("generationConfig", generationConfig);

        HttpEntity<String> entity = new HttpEntity<>(requestJson.toString(), headers);

        try {
            logger.info("Sending request to Gemini API...");
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JSONObject responseJson = new JSONObject(response.getBody());
            
            // Extract the response text
            String responseText = responseJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
            
            return responseText;
        } catch (Exception e) {
            logger.error("Error invoking Gemini API: {}", e.getMessage());
            throw new RuntimeException("Gemini API request failed: " + e.getMessage(), e);
        }
    }

    private String generateMockReview() {
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("architectureSummary", "The project follows a standard Spring Boot layer-based architecture with clean separation of controllers, services, and repositories. However, coupling between the web layer and entities could be reduced by introducing specialized DTOs for all data transfer points.");
        mockResponse.put("techStack", "Java 21, Spring Boot 3, Maven, Spring Data JPA, H2/MySQL.");
        mockResponse.put("securityAnalysis", "No high-priority security threats found. Some dependency injection issues were observed but there were no hardcoded secrets or credentials leaked in the source code.");
        
        JSONArray strengths = new JSONArray();
        strengths.put("Effective use of modern Java 21 features including records and clean patterns.");
        strengths.put("Structured packages with clear layer separation.");
        mockResponse.put("strengths", strengths);

        JSONArray weaknesses = new JSONArray();
        weaknesses.put("Direct exposure of entity classes in controllers in some locations.");
        weaknesses.put("Several duplicate validation routines.");
        mockResponse.put("weaknesses", weaknesses);

        JSONArray findings = new JSONArray();
        
        JSONObject finding1 = new JSONObject();
        finding1.put("title", "Avoid using direct Entities in Controllers");
        finding1.put("description", "Exposing database entities directly in rest interfaces breaks decoupling and exposes database layouts to client-side dependencies.");
        finding1.put("severity", "Warning");
        finding1.put("confidencePercentage", 95);
        finding1.put("reason", "Violates DTO architectural pattern and MVC model decoupling guidelines.");
        findings.put(finding1);

        JSONObject finding2 = new JSONObject();
        finding2.put("title", "Empty catch blocks detected in File Extraction");
        finding2.put("description", "File extraction utilities ignore ZipException errors and continue execution without logging exceptions.");
        finding2.put("severity", "Critical");
        finding2.put("confidencePercentage", 99);
        finding2.put("reason", "Swallowing exception errors can lead to silent data corruption and unexpected behaviors.");
        findings.put(finding2);
        
        mockResponse.put("findings", findings);
        
        mockResponse.put("finalVerdict", "The code exhibits high quality overall but requires quick fixes for input security validations and exception handling structure. Overall quality is robust.");
        
        // Scores
        mockResponse.put("codeQualityScore", 82.0);
        mockResponse.put("documentationScore", 75.0);
        mockResponse.put("maintainabilityScore", 80.0);
        mockResponse.put("overallScore", 79.0);

        return mockResponse.toString();
    }
}
