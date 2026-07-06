package com.devspec.service.testing;

import com.devspec.exception.BadRequestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class UnitTestExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(UnitTestExecutionService.class);

    private final String mavenHome;

    public UnitTestExecutionService(@Value("${devspec.maven.home}") String mavenHome) {
        this.mavenHome = mavenHome;
    }

    public static class TestRunResult {
        public int totalTests = 0;
        public int passed = 0;
        public int failed = 0;
        public int skipped = 0;
        public long executionTime = 0; // In ms
        public List<FailureDetail> failures = new ArrayList<>();
    }

    public static class FailureDetail {
        public String className;
        public String methodName;
        public String message;
        public String stackTrace;
    }

    public TestRunResult executeTests(File projectRoot) {
        TestRunResult result = new TestRunResult();

        // 1. Verify Maven test structures
        File testDir = new File(projectRoot, "src/test/java");
        if (!testDir.exists() || !testDir.isDirectory()) {
            logger.info("No test source directory found at src/test/java. Skipping test execution.");
            return result; // Returns empty result (all counts 0)
        }

        // 2. Prepare Maven command
        String mvnExecutable = mavenHome + File.separator + "bin" + File.separator + "mvn.cmd";
        File mvnFile = new File(mvnExecutable);
        if (!mvnFile.exists()) {
            // Fallback to system path if portable version is missing
            mvnExecutable = "mvn";
            logger.warn("Portable Maven not found at {}. Falling back to system 'mvn' command.", mavenHome);
        }

        logger.info("Executing tests for project: {} using {}", projectRoot.getAbsolutePath(), mvnExecutable);

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(projectRoot);
        // Using -Dmaven.test.failure.ignore=true so that Maven does not crash on failed tests and finishes generating Surefire reports
        pb.command(mvnExecutable, "clean", "test", "-Dmaven.test.failure.ignore=true");
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            
            // Log command outputs
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[MVN TEST] {}", line);
                }
            }

            int exitCode = process.waitFor();
            logger.info("Maven test process finished with exit code: {}", exitCode);

            // 3. Parse Surefire XML report files
            File surefireDir = new File(projectRoot, "target/surefire-reports");
            if (surefireDir.exists() && surefireDir.isDirectory()) {
                parseSurefireReports(surefireDir, result);
            } else {
                logger.warn("Surefire reports directory not found at target/surefire-reports. No reports parsed.");
            }

        } catch (Exception e) {
            logger.error("Failed to execute unit tests for project", e);
            throw new RuntimeException("Unit test execution failed: " + e.getMessage(), e);
        }

        return result;
    }

    private void parseSurefireReports(File surefireDir, TestRunResult runResult) {
        try (Stream<Path> paths = Files.walk(surefireDir.toPath())) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".xml"))
                 .forEach(path -> {
                     parseReportFile(path.toFile(), runResult);
                 });
        } catch (Exception e) {
            logger.error("Error reading Surefire reports: {}", e.getMessage());
        }
    }

    private void parseReportFile(File xmlFile, TestRunResult runResult) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            // Prevent XML external entity injection vulnerabilities (good security practice)
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            if (!root.getNodeName().equals("testsuite")) {
                return;
            }

            // Aggregate counts
            int tests = Integer.parseInt(root.getAttribute("tests"));
            int failures = Integer.parseInt(root.getAttribute("failures"));
            int errors = root.hasAttribute("errors") ? Integer.parseInt(root.getAttribute("errors")) : 0;
            int skipped = root.hasAttribute("skipped") ? Integer.parseInt(root.getAttribute("skipped")) : 0;
            double timeSec = Double.parseDouble(root.getAttribute("time"));

            int failedTotal = failures + errors;
            runResult.totalTests += tests;
            runResult.failed += failedTotal;
            runResult.skipped += skipped;
            runResult.passed += (tests - failedTotal - skipped);
            runResult.executionTime += (long) (timeSec * 1000); // convert to ms

            // Parse cases for failure details
            NodeList nList = doc.getElementsByTagName("testcase");
            for (int i = 0; i < nList.getLength(); i++) {
                Element testcase = (Element) nList.item(i);
                NodeList failureNodes = testcase.getElementsByTagName("failure");
                NodeList errorNodes = testcase.getElementsByTagName("error");
                
                Element issue = null;
                if (failureNodes.getLength() > 0) {
                    issue = (Element) failureNodes.item(0);
                } else if (errorNodes.getLength() > 0) {
                    issue = (Element) errorNodes.item(0);
                }

                if (issue != null) {
                    FailureDetail fd = new FailureDetail();
                    fd.className = testcase.getAttribute("classname");
                    fd.methodName = testcase.getAttribute("name");
                    fd.message = issue.getAttribute("message");
                    fd.stackTrace = issue.getTextContent();
                    runResult.failures.add(fd);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse surefire report file {}: {}", xmlFile.getName(), e.getMessage());
        }
    }

    public String toJson(TestRunResult result) {
        JSONObject obj = new JSONObject();
        obj.put("totalTests", result.totalTests);
        obj.put("passed", result.passed);
        obj.put("failed", result.failed);
        obj.put("skipped", result.skipped);
        obj.put("executionTime", result.executionTime);

        JSONArray failuresArr = new JSONArray();
        for (FailureDetail fd : result.failures) {
            JSONObject f = new JSONObject();
            f.put("className", fd.className);
            f.put("methodName", fd.methodName);
            f.put("message", fd.message);
            f.put("stackTrace", fd.stackTrace);
            failuresArr.put(f);
        }
        obj.put("failures", failuresArr);
        return obj.toString();
    }
}
