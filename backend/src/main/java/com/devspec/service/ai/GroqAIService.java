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

@Service("groq")
public class GroqAIService implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(GroqAIService.class);

    private final String apiKey;
    private final RestTemplate restTemplate;

    public GroqAIService(@Value("${groq.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String generateReview(String prompt) throws Exception {
        if (!StringUtils.hasText(apiKey) || apiKey.startsWith("${")) {
            logger.warn("Groq API key is not configured. Falling back to mock generator.");
            return generateMockReview();
        }

        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        JSONObject requestJson = new JSONObject();
        requestJson.put("model", "llama-3.1-70b-versatile");

        JSONArray messagesArray = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messagesArray.put(userMessage);
        requestJson.put("messages", messagesArray);

        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_object");
        requestJson.put("response_format", responseFormat);

        HttpEntity<String> entity = new HttpEntity<>(requestJson.toString(), headers);

        try {
            logger.info("Sending request to Groq API...");
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JSONObject responseJson = new JSONObject(response.getBody());
            
            // Extract the chat completion content
            String responseContent = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            
            return responseContent;
        } catch (Exception e) {
            logger.error("Error invoking Groq API: {}", e.getMessage());
            throw new RuntimeException("Groq API request failed: " + e.getMessage(), e);
        }
    }

    private String generateMockReview() {
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("architectureSummary", "The application uses a Spring Boot microservice layout with JPA persistence. Layer separation between controller, service, and database elements is clean, but data exchange can be decoupled more explicitly with specialized request/response DTOs.");
        mockResponse.put("techStack", "Java 21, Spring Boot 3, Maven, Spring Data JPA, H2/MySQL.");
        mockResponse.put("securityAnalysis", "All key paths are verified. There are no major leaks or exposed secret credentials in the scanned sources.");
        
        JSONArray strengths = new JSONArray();
        strengths.put("Effective layout structure based on Domain-Driven Design patterns.");
        strengths.put("Good test coverage checks in Maven build cycle.");
        mockResponse.put("strengths", strengths);

        JSONArray weaknesses = new JSONArray();
        weaknesses.put("Inconsistent package mappings in nested class files.");
        weaknesses.put("Exceptions in controller routes are logged using printStackTrace instead of unified logger.");
        mockResponse.put("weaknesses", weaknesses);

        JSONArray findings = new JSONArray();
        
        JSONObject finding1 = new JSONObject();
        finding1.put("title", "Avoid using printStackTrace()");
        finding1.put("description", "Using ex.printStackTrace() writes outputs to standard error streams, which does not write to system logging configs and leaks stack traces to the console.");
        finding1.put("severity", "Warning");
        finding1.put("confidencePercentage", 98);
        finding1.put("reason", "Violates industrial logger standard standards.");
        findings.put(finding1);

        JSONObject finding2 = new JSONObject();
        finding2.put("title", "Missing Path Traversal guards");
        finding2.put("description", "File extraction utilities should resolve all zip pathways using file.getCanonicalPath() validation checks.");
        finding2.put("severity", "Critical");
        finding2.put("confidencePercentage", 100);
        finding2.put("reason", "Path traversal exploits allow malicious files to override files outside of sandbox workspace folders.");
        findings.put(finding2);
        
        mockResponse.put("findings", findings);
        
        mockResponse.put("finalVerdict", "The project exhibits standard configurations. Addressing logging inconsistencies and validation paths will make it enterprise ready.");
        
        // Scores
        mockResponse.put("codeQualityScore", 85.0);
        mockResponse.put("documentationScore", 78.0);
        mockResponse.put("maintainabilityScore", 82.0);
        mockResponse.put("overallScore", 82.0);

        return mockResponse.toString();
    }
}
