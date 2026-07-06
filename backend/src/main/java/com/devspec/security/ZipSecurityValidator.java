package com.devspec.security;

import com.devspec.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipSecurityValidator {
    private static final Logger logger = LoggerFactory.getLogger(ZipSecurityValidator.class);

    // Limit uncompressed size to 150MB
    private static final long MAX_UNCOMPRESSED_SIZE = 150 * 1024 * 1024;
    // Limit max files count inside zip to 1000
    private static final int MAX_FILE_COUNT = 1000;

    @Value("${devspec.max-upload-bytes:52428800}") // 50MB
    private long maxUploadBytes;

    public void validateZipFile(File file) {
        // 1. Verify Magic Bytes for ZIP file (Signature: 50 4B 03 04)
        validateMagicBytes(file);

        // 2. Validate zip bomb & path traversals by traversing contents in read-only stream
        long totalUncompressedSize = 0;
        int fileCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                // Path traversal check (Zip Slip)
                if (name.contains("..") || name.startsWith("/") || name.contains("\\")) {
                    throw new BadRequestException("Illegal path traversal sequence found in ZIP entry name: " + name);
                }

                if (!entry.isDirectory()) {
                    fileCount++;
                    totalUncompressedSize += entry.getSize();

                    // Check bounds
                    if (fileCount > MAX_FILE_COUNT) {
                        throw new BadRequestException("Security Violation: ZIP contains too many files (Limit: " + MAX_FILE_COUNT + ")");
                    }
                    if (totalUncompressedSize > MAX_UNCOMPRESSED_SIZE) {
                        throw new BadRequestException("Security Violation: ZIP uncompressed size exceeds safety limits (Limit: 150 MB)");
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            logger.error("Failed to parse and validate ZIP archive structures", e);
            throw new BadRequestException("Failed to scan ZIP integrity: " + e.getMessage());
        }
    }

    private void validateMagicBytes(File file) {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            if (file.length() < 4) {
                throw new BadRequestException("Uploaded archive is too small to be valid");
            }
            int test = in.readInt();
            // 0x504B0304 is the Big-Endian representation of ZIP file header "PK\3\4"
            if (test != 0x504B0304) {
                logger.warn("Magic bytes mismatch. Header read was: 0x{}", Integer.toHexString(test));
                throw new BadRequestException("Invalid file format: upload is not a valid ZIP archive.");
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read magic signature bytes: " + e.getMessage());
        }
    }
}
