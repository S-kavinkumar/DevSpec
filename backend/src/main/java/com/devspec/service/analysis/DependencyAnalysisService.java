package com.devspec.service.analysis;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class DependencyAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisService.class);

    public static class DependencyInfo {
        public String groupId;
        public String artifactId;
        public String version;
        public String scope;
        public String packagePrefix;
    }

    public String analyzeDependencies(File projectRoot) {
        logger.info("Initializing dependency analysis for project in: {}", projectRoot.getAbsolutePath());
        JSONObject result = new JSONObject();

        List<DependencyInfo> dependencies = new ArrayList<>();
        List<String> javaImports = new ArrayList<>();
        boolean pomExists = false;

        File pomFile = new File(projectRoot, "pom.xml");
        if (pomFile.exists()) {
            pomExists = true;
            parsePomFile(pomFile, dependencies);
        }

        // 1. Scan Java files for import statements
        scanJavaImports(projectRoot, javaImports);

        // 2. Run dependency evaluations
        JSONArray duplicateList = new JSONArray();
        JSONArray outdatedList = new JSONArray();
        JSONArray unusedList = new JSONArray();
        JSONArray missingList = new JSONArray();

        Set<String> uniqueDeps = new HashSet<>();
        Map<String, String> commonVersionCheck = getOutdatedMap();

        for (DependencyInfo dep : dependencies) {
            String key = dep.groupId + ":" + dep.artifactId;

            // Check Duplicate
            if (!uniqueDeps.add(key)) {
                JSONObject dup = new JSONObject();
                dup.put("dependency", key);
                dup.put("description", "Dependency defined multiple times in pom.xml");
                duplicateList.put(dup);
            }

            // Check Outdated
            if (commonVersionCheck.containsKey(key)) {
                String latest = commonVersionCheck.get(key);
                if (dep.version != null && isOlderVersion(dep.version, latest)) {
                    JSONObject out = new JSONObject();
                    out.put("dependency", key);
                    out.put("current", dep.version);
                    out.put("latest", latest);
                    out.put("description", "Newer version available: " + latest);
                    outdatedList.put(out);
                }
            }

            // Check Unused (Exclude runtime / test scopes, and common plugins / database drivers)
            if (dep.packagePrefix != null && !"runtime".equalsIgnoreCase(dep.scope) && !"test".equalsIgnoreCase(dep.scope)) {
                boolean isUsed = false;
                for (String imp : javaImports) {
                    if (imp.startsWith(dep.packagePrefix)) {
                        isUsed = true;
                        break;
                    }
                }
                if (!isUsed) {
                    // Check if it is a starter that doesn't direct import, e.g. spring-boot-starter
                    if (!dep.artifactId.contains("starter")) {
                        JSONObject unused = new JSONObject();
                        unused.put("dependency", key);
                        unused.put("description", "No explicit imports found in Java source files (package prefix: " + dep.packagePrefix + ")");
                        unusedList.put(unused);
                    }
                }
            }
        }

        // Check Missing (e.g. standard packages imported but not declared)
        Map<String, String> commonPackageMappings = getCommonPackageMappings();
        Set<String> declaredKeys = new HashSet<>();
        for (DependencyInfo dep : dependencies) {
            declaredKeys.add(dep.groupId + ":" + dep.artifactId);
        }

        Set<String> flaggedImports = new HashSet<>();
        for (String imp : javaImports) {
            for (Map.Entry<String, String> entry : commonPackageMappings.entrySet()) {
                String pkg = entry.getKey();
                String depKey = entry.getValue();
                if (imp.startsWith(pkg) && !declaredKeys.contains(depKey) && flaggedImports.add(depKey)) {
                    JSONObject missing = new JSONObject();
                    missing.put("dependency", depKey);
                    missing.put("description", "Classes from '" + pkg + "' are imported, but dependency '" + depKey + "' is not declared explicitly in pom.xml");
                    missingList.put(missing);
                }
            }
        }

        // Compile metrics
        int total = dependencies.size();
        long compileCount = dependencies.stream().filter(d -> !"test".equalsIgnoreCase(d.scope) && !"runtime".equalsIgnoreCase(d.scope)).count();
        long testCount = dependencies.stream().filter(d -> "test".equalsIgnoreCase(d.scope)).count();

        result.put("pomExists", pomExists);
        result.put("totalDependencies", total);
        result.put("compileScopeCount", compileCount);
        result.put("testScopeCount", testCount);
        result.put("duplicateDependencies", duplicateList);
        result.put("outdatedDependencies", outdatedList);
        result.put("unusedDependencies", unusedList);
        result.put("missingDependencies", missingList);
        
        JSONObject stats = new JSONObject();
        stats.put("total", total);
        stats.put("outdatedCount", outdatedList.length());
        stats.put("unusedCount", unusedList.length());
        stats.put("duplicateCount", duplicateList.length());
        stats.put("missingCount", missingList.length());
        result.put("statistics", stats);

        return result.toString();
    }

    private void parsePomFile(File pomFile, List<DependencyInfo> dependencies) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable DTD loading for security
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(pomFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("dependency");
            for (int i = 0; i < nodeList.getLength(); i++) {
                org.w3c.dom.Node node = nodeList.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    DependencyInfo info = new DependencyInfo();
                    info.groupId = getTagValue("groupId", elem);
                    info.artifactId = getTagValue("artifactId", elem);
                    info.version = getTagValue("version", elem);
                    info.scope = getTagValue("scope", elem);
                    if (info.scope == null || info.scope.isEmpty()) {
                        info.scope = "compile";
                    }
                    info.packagePrefix = getPackagePrefix(info.groupId, info.artifactId);
                    dependencies.add(info);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse pom.xml dynamically", e);
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list != null && list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }

    private void scanJavaImports(File projectRoot, List<String> javaImports) {
        try (Stream<Path> paths = Files.walk(projectRoot.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try (Stream<String> lines = Files.lines(path)) {
                            lines.map(String::trim)
                                    .filter(line -> line.startsWith("import ") && line.endsWith(";"))
                                    .forEach(line -> {
                                        String imp = line.substring(7, line.length() - 1).trim();
                                        if (imp.startsWith("static ")) {
                                            imp = imp.substring(7).trim();
                                        }
                                        javaImports.add(imp);
                                    });
                        } catch (IOException e) {
                            // ignore file read errors
                        }
                    });
        } catch (IOException e) {
            logger.warn("Failed scanning imports under directory: {}", projectRoot.getAbsolutePath());
        }
    }

    private String getPackagePrefix(String groupId, String artifactId) {
        if (groupId == null) return null;
        if (groupId.startsWith("org.springframework")) {
            return "org.springframework";
        }
        if (groupId.startsWith("com.fasterxml.jackson")) {
            return "com.fasterxml.jackson";
        }
        if ("org.projectlombok".equals(groupId)) {
            return "lombok";
        }
        if ("org.json".equals(groupId)) {
            return "org.json";
        }
        if ("com.github.javaparser".equals(groupId)) {
            return "com.github.javaparser";
        }
        if ("net.sourceforge.pmd".equals(groupId)) {
            return "net.sourceforge.pmd";
        }
        if ("com.github.librepdf".equals(groupId) || "com.lowagie".equals(groupId)) {
            return "com.lowagie";
        }
        // Fallback guess: use groupId
        return groupId;
    }

    private boolean isOlderVersion(String current, String latest) {
        if (current == null || latest == null) return false;
        if (current.equals(latest)) return false;
        // Basic semantic comparison
        String[] currParts = current.split("[.-]");
        String[] lateParts = latest.split("[.-]");
        for (int i = 0; i < Math.min(currParts.length, lateParts.length); i++) {
            try {
                int cVal = Integer.parseInt(currParts[i]);
                int lVal = Integer.parseInt(lateParts[i]);
                if (cVal < lVal) return true;
                if (cVal > lVal) return false;
            } catch (NumberFormatException e) {
                // alphanumeric fallbacks
                int res = currParts[i].compareTo(lateParts[i]);
                if (res < 0) return true;
                if (res > 0) return false;
            }
        }
        return currParts.length < lateParts.length;
    }

    private Map<String, String> getOutdatedMap() {
        Map<String, String> map = new HashMap<>();
        map.put("org.springframework.boot:spring-boot-starter-web", "3.3.1");
        map.put("org.projectlombok:lombok", "1.18.32");
        map.put("com.h2database:h2", "2.2.224");
        map.put("org.json:json", "20240303");
        map.put("com.github.javaparser:javaparser-core", "3.26.1");
        map.put("net.sourceforge.pmd:pmd-core", "7.3.0");
        map.put("com.github.librepdf:openpdf", "1.3.40");
        return map;
    }

    private Map<String, String> getCommonPackageMappings() {
        Map<String, String> map = new HashMap<>();
        map.put("org.apache.commons.lang3", "org.apache.commons:commons-lang3");
        map.put("org.mockito", "org.mockito:mockito-core");
        map.put("com.google.gson", "com.google.code.gson:gson");
        return map;
    }
}
