package com.devspec.service.analysis;

import com.devspec.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
public class FileExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(FileExtractionService.class);

    private final String uploadBaseDir;
    private final com.devspec.security.ZipSecurityValidator zipSecurityValidator;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    public FileExtractionService(
            @Value("${devspec.upload.dir}") String uploadBaseDir,
            com.devspec.security.ZipSecurityValidator zipSecurityValidator) {
        this.uploadBaseDir = uploadBaseDir;
        this.zipSecurityValidator = zipSecurityValidator;
    }

    public File validateAndSaveZip(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds the limit of 50 MB");
        }

        // Create upload base directory if it doesn't exist
        File baseDir = new File(uploadBaseDir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File destFile = new File(baseDir, fileName);

        try {
            file.transferTo(destFile);
            logger.info("Saved temporary zip file: {}", destFile.getAbsolutePath());
            
            try {
                zipSecurityValidator.validateZipFile(destFile);
            } catch (Exception ex) {
                if (destFile.exists()) {
                    destFile.delete();
                }
                throw ex;
            }
            
            return destFile;
        } catch (IOException e) {
            logger.error("Failed to save uploaded file", e);
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    public File extractProject(File zipFile) {
        String uniqueId = UUID.randomUUID().toString();
        File extractionDir = new File(uploadBaseDir, uniqueId);
        
        if (!extractionDir.exists()) {
            extractionDir.mkdirs();
        }
        extractProjectSecurely(zipFile, extractionDir);
        return extractionDir;
    }

    public void extractProjectSecurely(File zipFile, File extractionDir) {
        // 1. Check integrity, encryption and format before extraction
        validateZipIntegrity(zipFile);

        // 2. Perform safe extraction
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                // Path Traversal Mitigation (Zip Slip Check)
                File newFile = newFile(extractionDir, entry);
                
                if (entry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Create parent directories if they don't exist
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    
                    // Write file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            logger.info("Successfully extracted zip project to: {}", extractionDir.getAbsolutePath());
            
            // 3. Verify Maven structures
            verifyMavenStructure(extractionDir);

        } catch (IOException e) {
            logger.error("Error extracting file: {}", zipFile.getAbsolutePath(), e);
            cleanupDirectory(extractionDir);
            throw new BadRequestException("Failed to extract project zip: " + e.getMessage());
        }
    }

    private void validateZipIntegrity(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            // Check if zip contains entries
            if (zip.size() == 0) {
                throw new BadRequestException("Uploaded zip file contains no entries");
            }
            
            // Iterate over entries to verify readability and check password protection
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // If encrypted, read will throw an exception
                try (InputStream is = zip.getInputStream(entry)) {
                    byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {
                        // Just checking readability
                    }
                } catch (ZipException e) {
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("encrypted")) {
                        throw new BadRequestException("Password-protected or encrypted ZIP files are not supported");
                    }
                    throw e;
                }
            }
        } catch (ZipException ze) {
            throw new BadRequestException("The uploaded file is a corrupted or invalid ZIP archive");
        } catch (IOException e) {
            throw new BadRequestException("IOException checking ZIP integrity: " + e.getMessage());
        }
    }

    private void verifyMavenStructure(File directory) {
        // Find pom.xml in the extracted folder or its subdirectories
        boolean hasPom = findPomFile(directory);
        if (!hasPom) {
            throw new BadRequestException("The uploaded project is not a valid Maven project (missing pom.xml)");
        }
    }

    private boolean findPomFile(File dir) {
        File pom = new File(dir, "pom.xml");
        if (pom.exists() && pom.isFile()) {
            return true;
        }
        
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    // Check direct subfolder as well (sometimes zipping adds a parent container folder)
                    File nestedPom = new File(child, "pom.xml");
                    if (nestedPom.exists() && nestedPom.isFile()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public File locateProjectRoot(File dir) {
        // Locates the exact path containing pom.xml
        File pom = new File(dir, "pom.xml");
        if (pom.exists() && pom.isFile()) {
            return dir;
        }

        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File nestedPom = new File(child, "pom.xml");
                    if (nestedPom.exists() && nestedPom.isFile()) {
                        return child;
                    }
                }
            }
        }
        return dir;
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        
        if (!destFilePath.startsWith(destDirPath + File.separator) && !destFilePath.equals(destDirPath)) {
            throw new ZipException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        
        return destFile;
    }

    public void cleanupFile(File file) {
        if (file != null && file.exists()) {
            if (file.delete()) {
                logger.info("Cleaned up file: {}", file.getAbsolutePath());
            } else {
                logger.warn("Failed to delete file: {}", file.getAbsolutePath());
            }
        }
    }

    public void cleanupDirectory(File dir) {
        if (dir != null && dir.exists()) {
            boolean success = FileSystemUtils.deleteRecursively(dir);
            if (success) {
                logger.info("Cleaned up temporary workspace directory: {}", dir.getAbsolutePath());
            } else {
                logger.warn("Failed to recursively clean directory: {}", dir.getAbsolutePath());
            }
        }
    }
}
