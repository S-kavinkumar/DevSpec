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
public class DatabaseAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseAnalysisService.class);

    private static final Pattern ENTITY_PATTERN = Pattern.compile("@Entity\\b");
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("interface\\s+\\w+\\s+extends\\s+(JpaRepository|CrudRepository|Repository)\\b");
    private static final Pattern TRANSACTIONAL_PATTERN = Pattern.compile("@Transactional\\b");
    private static final Pattern RELATION_PATTERN = Pattern.compile("@(OneToMany|ManyToOne|ManyToMany|OneToOne)\\b");
    
    // Pattern to catch fetch = FetchType.EAGER
    private static final Pattern EAGER_FETCH_PATTERN = Pattern.compile("fetch\\s*=\\s*FetchType\\.EAGER\\b");

    public String analyzeDatabase(File projectRoot) {
        logger.info("Initializing database and persistence analysis for project: {}", projectRoot.getAbsolutePath());
        JSONObject result = new JSONObject();

        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(projectRoot, javaFiles);

        int totalEntities = 0;
        int totalRepositories = 0;
        int totalRelationships = 0;
        int eagerFetchWarnings = 0;
        int transactionalMethods = 0;
        boolean hasTransactionalUsage = false;

        JSONArray entitiesDetail = new JSONArray();
        JSONArray warnings = new JSONArray();

        for (File file : javaFiles) {
            try {
                String content = Files.readString(file.toPath());
                String relativePath = projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                String className = file.getName().replace(".java", "");

                // Check Repositories
                if (REPOSITORY_PATTERN.matcher(content).find()) {
                    totalRepositories++;
                }

                // Check Transactional
                if (TRANSACTIONAL_PATTERN.matcher(content).find()) {
                    hasTransactionalUsage = true;
                    // Count total @Transactional occurrences
                    var txMatcher = TRANSACTIONAL_PATTERN.matcher(content);
                    while (txMatcher.find()) {
                        transactionalMethods++;
                    }
                }

                // Check Entities
                if (ENTITY_PATTERN.matcher(content).find()) {
                    totalEntities++;
                    JSONObject ent = new JSONObject();
                    ent.put("name", className);
                    ent.put("file", relativePath);

                    JSONArray rels = new JSONArray();
                    
                    String[] lines = content.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].trim();
                        Matcher relMatcher = RELATION_PATTERN.matcher(line);
                        if (relMatcher.find()) {
                            totalRelationships++;
                            String relType = relMatcher.group(1);
                            
                            // Check if eager fetch is defined in this annotation line or the surrounding block
                            boolean isEager = false;
                            StringBuilder block = new StringBuilder();
                            for (int j = Math.max(0, i - 1); j < Math.min(lines.length, i + 3); j++) {
                                block.append(lines[j]);
                            }
                            if (EAGER_FETCH_PATTERN.matcher(block.toString()).find()) {
                                isEager = true;
                                eagerFetchWarnings++;
                                
                                JSONObject warn = new JSONObject();
                                warn.put("entity", className);
                                warn.put("file", relativePath);
                                warn.put("line", i + 1);
                                warn.put("type", relType);
                                warn.put("description", "Performance risk: Eager fetch loading strategy detected. Consider changing to FetchType.LAZY to prevent N+1 query loops.");
                                warnings.put(warn);
                            }

                            JSONObject rObj = new JSONObject();
                            rObj.put("line", i + 1);
                            rObj.put("relationType", relType);
                            rObj.put("fetchType", isEager ? "EAGER" : "LAZY (Default)");
                            rels.put(rObj);
                        }
                    }

                    ent.put("relationships", rels);
                    entitiesDetail.put(ent);
                }
            } catch (IOException e) {
                // ignore
            }
        }

        // Database layer score calculation (0 - 100)
        double score = 100.0;
        if (totalEntities > 0 && totalRepositories == 0) {
            score -= 40; // No repositories found for entity entities
        }
        if (eagerFetchWarnings > 0) {
            score -= Math.min(30, eagerFetchWarnings * 10); // Deduct for eager fetching
        }
        if (!hasTransactionalUsage && totalEntities > 0) {
            score -= 20; // Deduct for missing @Transactional
        }
        score = Math.max(0.0, score);

        result.put("totalEntities", totalEntities);
        result.put("totalRepositories", totalRepositories);
        result.put("totalRelationships", totalRelationships);
        result.put("eagerFetchWarningsCount", eagerFetchWarnings);
        result.put("hasTransactionalUsage", hasTransactionalUsage);
        result.put("transactionalMethodsCount", transactionalMethods);
        result.put("databaseQualityScore", score);
        result.put("entities", entitiesDetail);
        result.put("databaseWarnings", warnings);

        return result.toString();
    }

    private void findJavaFiles(File root, List<File> files) {
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> files.add(path.toFile()));
        } catch (IOException e) {
            logger.warn("Traverse database files error under root: {}", root.getAbsolutePath());
        }
    }
}
