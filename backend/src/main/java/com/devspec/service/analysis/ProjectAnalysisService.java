package com.devspec.service.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class ProjectAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectAnalysisService.class);

    public static class Metrics {
        public int packagesCount = 0;
        public int classesCount = 0;
        public int interfacesCount = 0;
        public int enumsCount = 0;
        public int recordsCount = 0;
        public int methodsCount = 0;
        public int constructorsCount = 0;
        public int fieldsCount = 0;
        public int inheritanceCount = 0;
        public int implementedInterfacesCount = 0;
        public int annotationsCount = 0;
        public Set<String> packages = new HashSet<>();
        public Set<String> frameworks = new HashSet<>();
        public String language = "Java";
        public String buildTool = "Unknown";
        public String javaVersion = "Unknown";
        public java.util.List<String> scannedFiles = new java.util.ArrayList<>();
    }

    public Metrics analyzeProject(File rootDir) {
        Metrics metrics = new Metrics();
        detectBuildAndJavaVersion(rootDir, metrics);

        try (Stream<Path> paths = Files.walk(rootDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String relPath = rootDir.toPath().relativize(path).toString().replace('\\', '/');
                     metrics.scannedFiles.add(relPath);
                     if (relPath.endsWith(".java")) {
                         parseJavaFile(path.toFile(), metrics);
                     }
                 });
        } catch (IOException e) {
            logger.error("Failed to traverse files for project analysis", e);
        }

        // The packages count is the size of the packages set
        metrics.packagesCount = metrics.packages.size();

        return metrics;
    }

    private void parseJavaFile(File file, Metrics metrics) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            // Package declaration
            cu.getPackageDeclaration().ifPresent(pd -> {
                metrics.packages.add(pd.getNameAsString());
            });

            // Classes & Interfaces
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cid -> {
                if (cid.isInterface()) {
                    metrics.interfacesCount++;
                } else {
                    metrics.classesCount++;
                    if (cid.getExtendedTypes().isNonEmpty()) {
                        metrics.inheritanceCount++;
                    }
                    if (cid.getImplementedTypes().isNonEmpty()) {
                        metrics.implementedInterfacesCount += cid.getImplementedTypes().size();
                    }
                }
            });

            // Enums
            metrics.enumsCount += cu.findAll(EnumDeclaration.class).size();

            // Records
            metrics.recordsCount += cu.findAll(RecordDeclaration.class).size();

            // Methods
            metrics.methodsCount += cu.findAll(MethodDeclaration.class).size();

            // Constructors
            metrics.constructorsCount += cu.findAll(ConstructorDeclaration.class).size();

            // Fields
            metrics.fieldsCount += cu.findAll(FieldDeclaration.class).size();

            // Annotations
            cu.findAll(AnnotationDeclaration.class).forEach(ad -> {
                metrics.annotationsCount++;
            });

            // Check imports or annotations for framework markers
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cid -> {
                cid.getAnnotations().forEach(anno -> {
                    String name = anno.getNameAsString();
                    metrics.annotationsCount++;
                    if (name.equals("SpringBootApplication") || name.equals("RestController") || 
                        name.equals("Service") || name.equals("Component") || name.equals("Repository")) {
                        metrics.frameworks.add("Spring Boot");
                    }
                });
            });

        } catch (Exception e) {
            logger.warn("Skipping file {}. Parser error: {}", file.getName(), e.getMessage());
        }
    }

    private void detectBuildAndJavaVersion(File rootDir, Metrics metrics) {
        File pom = new File(rootDir, "pom.xml");
        File gradle = new File(rootDir, "build.gradle");
        File gradleKts = new File(rootDir, "build.gradle.kts");

        if (pom.exists() && pom.isFile()) {
            metrics.buildTool = "Maven";
            metrics.frameworks.add("Maven");
            extractJavaVersionFromPom(pom, metrics);
        } else if ((gradle.exists() && gradle.isFile()) || (gradleKts.exists() && gradleKts.isFile())) {
            metrics.buildTool = "Gradle";
            metrics.frameworks.add("Gradle");
            metrics.javaVersion = "Dynamic (Gradle)";
        } else {
            metrics.buildTool = "Unknown";
            metrics.javaVersion = "Unknown";
        }
    }

    private void extractJavaVersionFromPom(File pomFile, Metrics metrics) {
        try {
            String content = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
            
            // Basic regex / index checks to pull target java version
            String[] tags = {"<java.version>", "<maven.compiler.source>", "<maven.compiler.target>", "<release>"};
            for (String tag : tags) {
                String closingTag = tag.replace("<", "</");
                int start = content.indexOf(tag);
                if (start != -1) {
                    int end = content.indexOf(closingTag, start);
                    if (end != -1) {
                        metrics.javaVersion = content.substring(start + tag.length(), end).trim();
                        return;
                    }
                }
            }
            
            // Check spring boot starter parent version as proxy
            if (content.contains("spring-boot-starter-parent")) {
                metrics.frameworks.add("Spring Boot");
            }
            metrics.javaVersion = "17"; // Fallback standard
        } catch (Exception e) {
            logger.warn("Failed to extract Java Version from pom.xml: {}", e.getMessage());
            metrics.javaVersion = "Unknown";
        }
    }

    public String toJson(Metrics metrics) {
        JSONObject obj = new JSONObject();
        obj.put("language", metrics.language);
        obj.put("buildTool", metrics.buildTool);
        obj.put("javaVersion", metrics.javaVersion);
        obj.put("packagesCount", metrics.packagesCount);
        obj.put("classesCount", metrics.classesCount);
        obj.put("interfacesCount", metrics.interfacesCount);
        obj.put("enumsCount", metrics.enumsCount);
        obj.put("recordsCount", metrics.recordsCount);
        obj.put("methodsCount", metrics.methodsCount);
        obj.put("constructorsCount", metrics.constructorsCount);
        obj.put("fieldsCount", metrics.fieldsCount);
        obj.put("inheritanceCount", metrics.inheritanceCount);
        obj.put("implementedInterfacesCount", metrics.implementedInterfacesCount);
        obj.put("annotationsCount", metrics.annotationsCount);
        obj.put("frameworks", new JSONArray(metrics.frameworks));
        obj.put("packages", new JSONArray(metrics.packages));
        obj.put("scannedFiles", new JSONArray(metrics.scannedFiles));
        return obj.toString();
    }
}
