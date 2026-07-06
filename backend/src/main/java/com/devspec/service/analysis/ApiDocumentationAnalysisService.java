package com.devspec.service.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ApiDocumentationAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ApiDocumentationAnalysisService.class);

    private static final Pattern CONTROLLER_PATTERN = Pattern.compile("@(RestController|Controller)");
    private static final Pattern EXCEPTION_ADVICE_PATTERN = Pattern.compile("@(RestControllerAdvice|ControllerAdvice)");
    
    // Pattern to catch HTTP method mappings
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile(
            "@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\\s*(\\([^)]*\\))?"
    );

    public String analyzeApis(File projectRoot) {
        logger.info("Initializing API documentation analysis for project: {}", projectRoot.getAbsolutePath());
        JSONObject result = new JSONObject();

        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(projectRoot, javaFiles);

        int totalControllers = 0;
        int totalEndpoints = 0;
        int endpointsWithValidation = 0;
        boolean hasGlobalExceptionHandler = false;
        boolean hasSwaggerOpenApi = false;

        Map<String, Integer> methodCounts = new HashMap<>();
        methodCounts.put("GET", 0);
        methodCounts.put("POST", 0);
        methodCounts.put("PUT", 0);
        methodCounts.put("DELETE", 0);
        methodCounts.put("PATCH", 0);
        methodCounts.put("OTHER", 0);

        JSONArray controllersDetail = new JSONArray();

        for (File file : javaFiles) {
            try {
                String content = Files.readString(file.toPath());
                String relativePath = projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/');

                if (EXCEPTION_ADVICE_PATTERN.matcher(content).find()) {
                    hasGlobalExceptionHandler = true;
                }

                if (CONTROLLER_PATTERN.matcher(content).find()) {
                    totalControllers++;
                    JSONObject ctrl = new JSONObject();
                    ctrl.put("file", relativePath);
                    ctrl.put("name", file.getName().replace(".java", ""));

                    JSONArray endPoints = new JSONArray();
                    
                    // Simple scanning for endpoints
                    String[] lines = content.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();
                        Matcher matcher = ENDPOINT_PATTERN.matcher(line);
                        if (matcher.find()) {
                            totalEndpoints++;
                            String annotation = matcher.group(1);
                            String mappingValue = matcher.group(2) != null ? matcher.group(2) : "";
                            
                            // Map HTTP verb
                            String verb = "OTHER";
                            if (annotation.startsWith("Get")) verb = "GET";
                            else if (annotation.startsWith("Post")) verb = "POST";
                            else if (annotation.startsWith("Put")) verb = "PUT";
                            else if (annotation.startsWith("Delete")) verb = "DELETE";
                            else if (annotation.startsWith("Patch")) verb = "PATCH";

                            methodCounts.put(verb, methodCounts.getOrDefault(verb, 0) + 1);

                            // Check validation on parameters in next few lines (signature)
                            boolean hasValidation = false;
                            StringBuilder methodSignature = new StringBuilder();
                            for (int j = i; j < Math.min(lines.length, i + 5); j++) {
                                methodSignature.append(lines[j]);
                            }
                            if (methodSignature.toString().contains("@Valid") || methodSignature.toString().contains("@Validated")) {
                                hasValidation = true;
                                endpointsWithValidation++;
                            }

                            JSONObject ep = new JSONObject();
                            ep.put("line", i + 1);
                            ep.put("httpMethod", verb);
                            ep.put("mapping", mappingValue.replaceAll("[\"()]", ""));
                            ep.put("validated", hasValidation);
                            endPoints.put(ep);
                        }
                    }

                    ctrl.put("endpoints", endPoints);
                    controllersDetail.put(ctrl);
                }
            } catch (IOException e) {
                // ignore file errors
            }
        }

        // Check pom.xml for Swagger/OpenAPI setup
        File pomFile = new File(projectRoot, "pom.xml");
        if (pomFile.exists()) {
            try {
                String pomContent = Files.readString(pomFile.toPath());
                if (pomContent.contains("springdoc-openapi") || pomContent.contains("swagger-ui") || pomContent.contains("io.swagger")) {
                    hasSwaggerOpenApi = true;
                }
            } catch (IOException e) {
                // ignore
            }
        }

        // API Quality calculations (0 - 100)
        double score = 100.0;
        if (totalEndpoints > 0) {
            double validationRatio = (double) endpointsWithValidation / totalEndpoints;
            // 40 points for validation
            score -= (1.0 - validationRatio) * 40;
        } else {
            score -= 40; // No endpoints, quality is lower
        }
        if (!hasGlobalExceptionHandler) {
            score -= 30; // 30 points for global exception advisor
        }
        if (!hasSwaggerOpenApi) {
            score -= 30; // 30 points for OpenAPI swagger docs
        }
        score = Math.max(0.0, score);

        result.put("totalControllers", totalControllers);
        result.put("totalEndpoints", totalEndpoints);
        result.put("validationPercentage", totalEndpoints > 0 ? Math.round((endpointsWithValidation * 100.0) / totalEndpoints) : 0);
        result.put("hasGlobalExceptionHandler", hasGlobalExceptionHandler);
        result.put("hasSwaggerOpenApi", hasSwaggerOpenApi);
        result.put("apiQualityScore", score);
        result.put("methodDistribution", new JSONObject(methodCounts));
        result.put("controllers", controllersDetail);

        return result.toString();
    }

    private void findJavaFiles(File root, List<File> files) {
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> files.add(path.toFile()));
        } catch (IOException e) {
            logger.warn("Traverse source files error under root: {}", root.getAbsolutePath());
        }
    }
}
