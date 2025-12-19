package com.platform.media.provider;

import com.platform.media.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class LocalFileSystemProvider implements MediaProvider {

    @Value("${media.local.base-path:uploads/media}")
    private String basePath;

    @Value("${media.local.base-url:http://localhost:8080/api/media/files}")
    private String baseUrl;

    @Value("${media.storage.host:http://localhost:8080}")
    private String storageHost;

    @Override
    public UploadResult upload(FilePayload payload) {
        try {
            // Resolve base path to absolute path
            Path baseDir = Paths.get(basePath).toAbsolutePath();
            
            // Create directory structure - ensure we stay within base directory
            String folderPath = payload.getFolderPath();
            if (folderPath == null || folderPath.trim().isEmpty() || folderPath.equals("/")) {
                folderPath = "";
            }
            // Sanitize folder path to prevent directory traversal
            folderPath = folderPath.replaceAll("\\.\\.", "").replaceAll("//+", "/");
            if (folderPath.startsWith("/")) {
                folderPath = folderPath.substring(1);
            }
            
            Path uploadDir = baseDir.resolve(folderPath);
            log.debug("Creating upload directory: {}", uploadDir);
            
            // Security check: ensure upload directory is within base directory
            if (!uploadDir.normalize().startsWith(baseDir.normalize())) {
                throw new SecurityException("Invalid folder path: " + payload.getFolderPath());
            }
            
            Files.createDirectories(uploadDir);

            // Generate unique filename
            String fileExtension = getFileExtension(payload.getFilename());
            String uniqueFilename = UUID.randomUUID().toString() + 
                (fileExtension.isEmpty() ? "" : "." + fileExtension);
            
            Path filePath = uploadDir.resolve(uniqueFilename);
            log.debug("Uploading file to: {}", filePath);
            
            // Save file
            if (payload.getMultipartFile() != null) {
                payload.getMultipartFile().transferTo(filePath.toFile());
            } else if (payload.getInputStream() != null) {
                Files.copy(payload.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Calculate file hash
            String fileHash = calculateFileHash(filePath);
            
            // Generate URLs - ensure proper relative path calculation
            Path normalizedBasePath = baseDir.normalize();
            Path normalizedFilePath = filePath.normalize();
            
            // Security check: ensure file is within base directory
            if (!normalizedFilePath.startsWith(normalizedBasePath)) {
                throw new SecurityException("File path outside base directory");
            }
            
            String relativePath = normalizedBasePath.relativize(normalizedFilePath).toString().replace("\\", "/");
            // Clean up any remaining path traversal sequences and ensure clean URL
            relativePath = relativePath.replaceAll("\\.\\./", "").replaceAll("//+", "/");
            String publicUrl = storageHost + "/api/media/files/" + relativePath;
            
            log.debug("Generated URLs - relativePath: {}, publicUrl: {}, storageHost: {}", relativePath, publicUrl, storageHost);

            return UploadResult.builder()
                .providerKey(relativePath)
                .publicUrl(publicUrl)
                .privateUrl(publicUrl)
                .fileSize(Files.size(filePath))
                .mimeType(payload.getMimeType())
                .fileHash(fileHash)
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("Failed to upload file to local storage", e);
            return UploadResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    public void delete(String providerKey) {
        try {
            Path baseDir = Paths.get(basePath).toAbsolutePath();
            Path filePath = baseDir.resolve(providerKey);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", providerKey, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public MediaUrl generateUrl(String providerKey, AccessType accessType) {
        String url = storageHost + "/api/media/files/" + providerKey;
        
        return MediaUrl.builder()
            .url(url)
            .accessType(accessType)
            .isTemporary(false)
            .build();
    }

    @Override
    public boolean exists(String providerKey) {
        Path baseDir = Paths.get(basePath).toAbsolutePath();
        Path filePath = baseDir.resolve(providerKey);
        return Files.exists(filePath);
    }

    @Override
    public FileMetadata getMetadata(String providerKey) {
        try {
            Path baseDir = Paths.get(basePath).toAbsolutePath();
            Path filePath = baseDir.resolve(providerKey);
            if (!Files.exists(filePath)) {
                return FileMetadata.builder().exists(false).build();
            }

            return FileMetadata.builder()
                .filename(filePath.getFileName().toString())
                .fileSize(Files.size(filePath))
                .lastModified(LocalDateTime.now()) // Simplified
                .exists(true)
                .build();

        } catch (IOException e) {
            log.error("Failed to get metadata for file: {}", providerKey, e);
            return FileMetadata.builder().exists(false).build();
        }
    }

    @Override
    public String getProviderType() {
        return "LOCAL";
    }

    @Override
    public boolean validateConfiguration() {
        try {
            Path baseDir = Paths.get(basePath).toAbsolutePath();
            log.info("Validating media storage directory: {}", baseDir);
            if (!Files.exists(baseDir)) {
                log.info("Creating media storage directory: {}", baseDir);
                Files.createDirectories(baseDir);
            }
            boolean isWritable = Files.isWritable(baseDir);
            log.info("Media storage directory writable: {}", isWritable);
            return isWritable;
        } catch (Exception e) {
            log.error("Local storage configuration validation failed for path: {}", basePath, e);
            return false;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String calculateFileHash(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to calculate file hash", e);
            return null;
        }
    }
}