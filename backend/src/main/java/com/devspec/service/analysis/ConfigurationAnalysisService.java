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
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ConfigurationAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationAnalysisService.class);

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|secret|passwd|token|apikey|private_key|client_secret|aws_key|secret_key)\\s*[:=]\\s*['\"]?[a-zA-Z0-9_/+=]{6,}['\"]?"
    );

    public String analyzeConfigurations(File projectRoot) {
        logger.info("Initializing configuration analysis for project: {}", projectRoot.getAbsolutePath());
        JSONObject result = new JSONObject();

        JSONArray issues = new JSONArray();
        List<File> configFiles = new ArrayList<>();
        
        // Find configuration files
        findConfigFiles(projectRoot, configFiles);

        boolean hasDatabaseConfig = false;
        boolean hasDatabaseDependency = false;

        for (File file : configFiles) {
            String relativePath = projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/');
            String name = file.getName();

            try {
                String content = Files.readString(file.toPath());

                // Check for hardcoded secrets in configurations
                var matcher = SECRET_PATTERN.matcher(content);
                while (matcher.find()) {
                    String match = matcher.group();
                    if (!match.contains("${") && !match.contains("@")) { // Skip placeholders/vars
                        JSONObject iss = new JSONObject();
                        iss.put("file", relativePath);
                        iss.put("category", "Security");
                        iss.put("riskLevel", "Critical");
                        iss.put("title", "Hardcoded Configuration Secret");
                        iss.put("description", "A potential plain-text secret or credential was found hardcoded: '" + match.split("[:=]")[0].trim() + "'. Use environment variables or security vault placeholders.");
                        issues.put(iss);
                    }
                }

                // File specific parsing checks
                if ("application.properties".equals(name)) {
                    checkPropertiesFile(content, relativePath, issues);
                    if (content.contains("spring.datasource.url")) {
                        hasDatabaseConfig = true;
                    }
                } else if ("application.yml".equals(name) || "application.yaml".equals(name)) {
                    checkYmlFile(content, relativePath, issues);
                    if (content.contains("datasource:") && content.contains("url:")) {
                        hasDatabaseConfig = true;
                    }
                } else if ("pom.xml".equals(name)) {
                    if (content.contains("mysql-connector") || content.contains("h2") || content.contains("spring-boot-starter-data-jpa")) {
                        hasDatabaseDependency = true;
                    }
                    checkPomConfig(content, relativePath, issues);
                }

            } catch (IOException e) {
                logger.warn("Skipping configuration scan for file {}: {}", name, e.getMessage());
            }
        }

        // Database gap check
        if (hasDatabaseDependency && !hasDatabaseConfig) {
            JSONObject iss = new JSONObject();
            iss.put("file", "application.properties / application.yml");
            iss.put("category", "Architecture");
            iss.put("riskLevel", "Warning");
            iss.put("title", "Missing Database Connection Configs");
            iss.put("description", "Database drivers are declared in pom.xml, but datasource properties (spring.datasource.url) are missing in project configurations.");
            issues.put(iss);
        }

        result.put("scannedFilesCount", configFiles.size());
        result.put("configIssues", issues);
        result.put("databaseConfigured", hasDatabaseConfig);

        return result.toString();
    }

    private void findConfigFiles(File root, List<File> files) {
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String name = path.getFileName().toString();
                     if ("application.properties".equals(name) ||
                             "application.yml".equals(name) ||
                             "application.yaml".equals(name) ||
                             "pom.xml".equals(name) ||
                             "build.gradle".equals(name)) {
                         files.add(path.toFile());
                     }
                 });
        } catch (IOException e) {
            logger.warn("Traverse configurations error under root: {}", root.getAbsolutePath());
        }
    }

    private void checkPropertiesFile(String content, String relativePath, JSONArray issues) {
        String[] lines = content.split("\n");
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx == -1) {
                idx = line.indexOf(':');
            }
            if (idx == -1) {
                // Syntax Error
                JSONObject iss = new JSONObject();
                iss.put("file", relativePath);
                iss.put("category", "Code Quality");
                iss.put("riskLevel", "Warning");
                iss.put("title", "Malformed Properties Config");
                iss.put("description", "Line " + (i + 1) + " does not follow standard key=value syntax: '" + line + "'");
                issues.put(iss);
                continue;
            }

            String key = line.substring(0, idx).trim();
            if (!keys.add(key)) {
                JSONObject iss = new JSONObject();
                iss.put("file", relativePath);
                iss.put("category", "Code Quality");
                iss.put("riskLevel", "Warning");
                iss.put("title", "Duplicate Property Definition");
                iss.put("description", "Configuration key '" + key + "' is defined multiple times. The last value will overwrite previous values.");
                issues.put(iss);
            }
        }
    }

    private void checkYmlFile(String content, String relativePath, JSONArray issues) {
        // Basic indentation validation
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                continue;
            }
            // YAML tabs are forbidden
            if (line.contains("\t")) {
                JSONObject iss = new JSONObject();
                iss.put("file", relativePath);
                iss.put("category", "Code Quality");
                iss.put("riskLevel", "Critical");
                iss.put("title", "Forbidden Tab Character in YAML");
                iss.put("description", "Tab character detected on line " + (i + 1) + ". YAML requires spaces for indentation.");
                issues.put(iss);
            }
        }
    }

    private void checkPomConfig(String content, String relativePath, JSONArray issues) {
        // Check compiler version
        if (content.contains("<java.version>1.8</java.version>") || content.contains("<java.version>8</java.version>")) {
            JSONObject iss = new JSONObject();
            iss.put("file", relativePath);
            iss.put("category", "Maintainability");
            iss.put("riskLevel", "Warning");
            iss.put("title", "Legacy Java Version Configured");
            iss.put("description", "Java 8 is configured. Consider upgrading to an LTS release (Java 17 or 21) for performance, language features, and support.");
            issues.put(iss);
        }
    }
}
