package com.platform.media.provider;

import com.platform.entity.MediaProviderConfig;
import com.platform.media.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class CloudflareR2Provider implements MediaProvider {

    private MediaProviderConfig config;
    private CloudflareR2Config r2Config;
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    public CloudflareR2Provider() {
        // Default constructor for Spring
    }

    public CloudflareR2Provider(MediaProviderConfig config) {
        this.config = config;
        this.r2Config = parseConfig(config);
        this.s3Client = initializeS3Client();
        this.s3Presigner = initializeS3Presigner();
    }

    @Override
    public UploadResult upload(FilePayload payload) {
        try {
            if (s3Client == null) {
                log.error("Cloudflare R2 service not initialized. Configuration details:");
                log.error("Account ID: {}", r2Config != null ? r2Config.getAccountId() : "null");
                log.error("Bucket Name: {}", r2Config != null ? r2Config.getBucketName() : "null");
                log.error("Access Key ID: {}", r2Config != null ? r2Config.getAccessKeyId() : "null");
                return UploadResult.builder()
                    .success(false)
                    .errorMessage("Cloudflare R2 service not initialized. Please check configuration.")
                    .build();
            }

            // Generate unique key for the file
            String fileKey = generateFileKey(payload);
            log.info("Generated file key: {} for file: {}", fileKey, payload.getFilename());
            
            // Prepare metadata
            Map<String, String> metadata = Map.of(
                "original-filename", payload.getFilename(),
                "upload-time", String.valueOf(System.currentTimeMillis()),
                "corporate-id", payload.getCorporateId() != null ? payload.getCorporateId() : "unknown"
            );

            // Create put object request
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(fileKey)
                .contentType(payload.getMimeType())
                .contentLength(payload.getFileSize())
                .metadata(metadata);

            // Set public access if configured
            if (r2Config.isPublicAccess()) {
                requestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
                log.info("Setting public read access for file: {}", fileKey);
            }

            PutObjectRequest putRequest = requestBuilder.build();

            // Upload file
            RequestBody requestBody = RequestBody.fromInputStream(
                payload.getMultipartFile().getInputStream(),
                payload.getFileSize()
            );

            log.info("Uploading file to bucket: {} with key: {}", r2Config.getBucketName(), fileKey);
            s3Client.putObject(putRequest, requestBody);
            log.info("File upload completed successfully");

            // Generate URLs
            String publicUrl = null;
            String privateUrl = null; // Don't generate presigned URL during upload to avoid DB constraint issues

            if (r2Config.isPublicAccess()) {
                publicUrl = generatePublicUrl(fileKey);
                privateUrl = publicUrl; // Use public URL as private URL if public access is enabled
                log.info("Generated public URL: {}", publicUrl);
            } else {
                // For private files, we'll generate presigned URLs on-demand via generateUrl() method
                // Store a placeholder or the base URL pattern instead of the actual presigned URL
                privateUrl = String.format("r2://%s/%s", r2Config.getBucketName(), fileKey);
                log.info("File uploaded as private, stored placeholder URL: {}", privateUrl);
            }

            log.info("File uploaded to Cloudflare R2: {} (Key: {})", payload.getFilename(), fileKey);

            return UploadResult.builder()
                .success(true)
                .providerKey(fileKey)
                .publicUrl(publicUrl)
                .privateUrl(privateUrl)
                .fileHash(generateFileHash(payload))
                .build();

        } catch (Exception e) {
            log.error("Failed to upload file to Cloudflare R2: {}", e.getMessage(), e);
            
            // Add specific error handling for common issues
            if (e.getMessage().contains("SSL") || e.getMessage().contains("handshake")) {
                log.error("SSL handshake failure detected. Please check:");
                log.error("1. Account ID is correct: {}", r2Config != null ? r2Config.getAccountId() : "null");
                log.error("2. Endpoint URL is accessible: {}", r2Config != null ? r2Config.getEndpointUrl() : "null");
                log.error("3. Network connectivity to Cloudflare R2");
            }
            
            return UploadResult.builder()
                .success(false)
                .errorMessage("Upload failed: " + e.getMessage())
                .build();
        }
    }

    @Override
    public void delete(String providerKey) {
        try {
            if (s3Client == null) {
                String errorMsg = "Cloudflare R2 service not initialized, cannot delete file: " + providerKey;
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            if (providerKey == null || providerKey.trim().isEmpty()) {
                String errorMsg = "Provider key is null or empty, cannot delete file";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            log.info("Attempting to delete file from Cloudflare R2 - Bucket: {}, Key: {}", 
                     r2Config.getBucketName(), providerKey);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(providerKey)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info("File successfully deleted from Cloudflare R2: {}", providerKey);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to delete file '%s' from Cloudflare R2 bucket '%s': %s", 
                                          providerKey, 
                                          r2Config != null ? r2Config.getBucketName() : "unknown", 
                                          e.getMessage());
            log.error(errorMsg, e);
            
            // Provide more specific error information
            if (e.getMessage().contains("NoSuchKey")) {
                throw new RuntimeException("File not found in Cloudflare R2: " + providerKey);
            } else if (e.getMessage().contains("AccessDenied")) {
                throw new RuntimeException("Access denied when deleting file from Cloudflare R2. Check credentials and permissions.");
            } else if (e.getMessage().contains("NoSuchBucket")) {
                throw new RuntimeException("Bucket not found in Cloudflare R2: " + (r2Config != null ? r2Config.getBucketName() : "unknown"));
            } else {
                throw new RuntimeException(errorMsg);
            }
        }
    }

    @Override
    public MediaUrl generateUrl(String providerKey, AccessType accessType) {
        try {
            if (s3Client == null) {
                return MediaUrl.builder()
                    .url("#")
                    .expiresAt(null)
                    .isTemporary(false)
                    .build();
            }

            String url;
            LocalDateTime expiresAt = null;
            boolean isTemporary = false;

            if (accessType == AccessType.PUBLIC && r2Config.isPublicAccess()) {
                // Generate public URL
                url = generatePublicUrl(providerKey);
            } else {
                // Generate presigned URL for private access
                url = generatePresignedUrl(providerKey);
                expiresAt = LocalDateTime.now().plusHours(1); // 1 hour expiry
                isTemporary = true;
            }

            return MediaUrl.builder()
                .url(url)
                .expiresAt(expiresAt)
                .isTemporary(isTemporary)
                .build();

        } catch (Exception e) {
            log.error("Failed to generate URL for Cloudflare R2 file: {}", e.getMessage());
            return MediaUrl.builder()
                .url("#")
                .expiresAt(null)
                .isTemporary(false)
                .build();
        }
    }

    @Override
    public boolean exists(String providerKey) {
        try {
            if (s3Client == null) {
                return false;
            }

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(providerKey)
                .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            log.debug("File does not exist in Cloudflare R2: {}", providerKey);
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence in Cloudflare R2: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public FileMetadata getMetadata(String providerKey) {
        try {
            if (s3Client == null) {
                return createDefaultMetadata();
            }

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(providerKey)
                .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);

            String originalFilename = response.metadata().get("original-filename");
            if (originalFilename == null) {
                // Extract filename from key if metadata is not available
                originalFilename = extractFilenameFromKey(providerKey);
            }

            return FileMetadata.builder()
                .filename(originalFilename)
                .fileSize(response.contentLength())
                .mimeType(response.contentType())
                .lastModified(response.lastModified() != null ? 
                    LocalDateTime.ofInstant(response.lastModified(), ZoneId.systemDefault()) : null)
                .build();

        } catch (Exception e) {
            log.error("Failed to get metadata for Cloudflare R2 file: {}", e.getMessage());
            return createDefaultMetadata();
        }
    }

    @Override
    public String getProviderType() {
        return "CLOUDFLARE_R2";
    }

    @Override
    public boolean validateConfiguration() {
        try {
            if (config == null) {
                log.warn("Configuration is null");
                return false;
            }

            CloudflareR2Config testConfig = parseConfig(config);
            if (testConfig.getAccessKeyId() == null || testConfig.getAccessKeyId().isEmpty() ||
                testConfig.getSecretAccessKey() == null || testConfig.getSecretAccessKey().isEmpty() ||
                testConfig.getBucketName() == null || testConfig.getBucketName().isEmpty()) {
                log.warn("Missing required configuration fields");
                return false;
            }

            log.info("Validating Cloudflare R2 configuration:");
            log.info("Account ID: {}", testConfig.getAccountId());
            log.info("Bucket Name: {}", testConfig.getBucketName());
            log.info("Endpoint URL: {}", testConfig.getEndpointUrl());
            log.info("Access Key ID: {}", testConfig.getAccessKeyId());

            // Try to initialize the client and test connection
            S3Client testClient = null;
            try {
                testClient = createS3Client(testConfig);
                
                // Test by listing objects (this will fail if credentials are invalid)
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(testConfig.getBucketName())
                    .maxKeys(1)
                    .build();
                
                ListObjectsV2Response response = testClient.listObjectsV2(listRequest);
                log.info("Successfully connected to Cloudflare R2. Found {} objects in bucket.", response.contents().size());
                
                return true;

            } catch (Exception e) {
                log.error("Cloudflare R2 connection test failed: {}", e.getMessage(), e);
                
                // Check for specific SSL handshake error
                if (e.getMessage().contains("SSL") || e.getMessage().contains("handshake") || 
                    e.getMessage().contains("PKIX") || e.getMessage().contains("certificate")) {
                    log.error("SSL/TLS handshake failure detected. This might be due to:");
                    log.error("1. Incorrect endpoint URL format");
                    log.error("2. Network connectivity issues");
                    log.error("3. Firewall blocking HTTPS connections");
                    log.error("4. Invalid account ID in endpoint URL");
                }
                
                return false;
            } finally {
                if (testClient != null) {
                    try {
                        testClient.close();
                    } catch (Exception e) {
                        log.warn("Error closing test client: {}", e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Cloudflare R2 configuration validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a configured instance of CloudflareR2Provider
     */
    public static CloudflareR2Provider create(MediaProviderConfig config) {
        return new CloudflareR2Provider(config);
    }

    // Private helper methods

    private CloudflareR2Config parseConfig(MediaProviderConfig config) {
        if (config == null || config.getConfiguration() == null) {
            return CloudflareR2Config.builder()
                .region("auto")
                .publicAccess(false)
                .build();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = mapper.readValue(config.getConfiguration(), Map.class);
            return CloudflareR2Config.fromMap(configMap);
        } catch (Exception e) {
            log.error("Failed to parse Cloudflare R2 configuration: {}", e.getMessage());
            return CloudflareR2Config.builder()
                .region("auto")
                .publicAccess(false)
                .build();
        }
    }

    private S3Client initializeS3Client() {
        try {
            if (r2Config == null) {
                log.debug("Cloudflare R2 configuration is null, service not initialized");
                return null;
            }
            if (r2Config.getAccessKeyId() == null || r2Config.getAccessKeyId().isEmpty() ||
                r2Config.getSecretAccessKey() == null || r2Config.getSecretAccessKey().isEmpty()) {
                log.debug("Cloudflare R2 credentials are empty, service not initialized");
                return null;
            }
            return createS3Client(r2Config);
        } catch (Exception e) {
            log.error("Failed to initialize Cloudflare R2 S3 client: {}", e.getMessage());
            return null;
        }
    }

    private S3Presigner initializeS3Presigner() {
        try {
            if (r2Config == null) {
                log.debug("Cloudflare R2 configuration is null, presigner not initialized");
                return null;
            }
            if (r2Config.getAccessKeyId() == null || r2Config.getAccessKeyId().isEmpty() ||
                r2Config.getSecretAccessKey() == null || r2Config.getSecretAccessKey().isEmpty()) {
                log.debug("Cloudflare R2 credentials are empty, presigner not initialized");
                return null;
            }
            return createS3Presigner(r2Config);
        } catch (Exception e) {
            log.error("Failed to initialize Cloudflare R2 S3 presigner: {}", e.getMessage());
            return null;
        }
    }

    private S3Client createS3Client(CloudflareR2Config config) {
        if (config.getAccessKeyId() == null || config.getAccessKeyId().isEmpty() ||
            config.getSecretAccessKey() == null || config.getSecretAccessKey().isEmpty()) {
            throw new IllegalArgumentException("Access Key ID and Secret Access Key are required");
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            config.getAccessKeyId(),
            config.getSecretAccessKey()
        );

        // Ensure region is not null or empty - use "auto" as default for R2
        String region = config.getRegion();
        if (region == null || region.trim().isEmpty()) {
            region = "auto";
        }

        String endpointUrl = config.getEndpointUrl();
        log.info("=== Cloudflare R2 Client Configuration ===");
        log.info("Endpoint URL: {}", endpointUrl);
        log.info("Region: {}", region);
        log.info("Account ID: {}", config.getAccountId());
        log.info("Access Key ID: {}", config.getAccessKeyId());
        log.info("Bucket Name: {}", config.getBucketName());
        log.info("Public Access: {}", config.isPublicAccess());
        log.info("Custom Domain: {}", config.getCustomDomain());
        log.info("==========================================");

        try {
            // Try multiple endpoint formats to find one that works
            String[] endpointFormats = config.getAlternativeEndpoints();
            
            Exception lastException = null;
            
            for (String endpoint : endpointFormats) {
                try {
                    log.info("Attempting to create S3 client with endpoint: {}", endpoint);
                    
                    S3Client client = S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.of(region))
                        .forcePathStyle(true) // Required for R2
                        .build();
                    
                    log.info("Successfully created S3 client with endpoint: {}", endpoint);
                    return client;
                    
                } catch (Exception e) {
                    log.warn("Failed to create S3 client with endpoint {}: {}", endpoint, e.getMessage());
                    lastException = e;
                    
                    // Log specific SSL errors
                    if (e.getMessage().contains("SSL") || e.getMessage().contains("handshake")) {
                        log.error("SSL handshake failure with endpoint: {}", endpoint);
                        log.error("SSL Error details: {}", e.getMessage());
                    }
                }
            }
            
            // If all endpoints failed, throw the last exception
            throw lastException != null ? lastException : new RuntimeException("Failed to create S3 client with any endpoint format");
            
        } catch (Exception e) {
            log.error("Failed to create S3 client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Cloudflare R2 client: " + e.getMessage(), e);
        }
    }

    private S3Presigner createS3Presigner(CloudflareR2Config config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            config.getAccessKeyId(),
            config.getSecretAccessKey()
        );

        // Ensure region is not null or empty - use "auto" as default for R2
        String region = config.getRegion();
        if (region == null || region.trim().isEmpty()) {
            region = "auto";
        }

        return S3Presigner.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(config.getEndpointUrl()))
            .region(Region.of(region))
            .build();
    }

    private String generateFileKey(FilePayload payload) {
        String uuid = UUID.randomUUID().toString();
        String filename = payload.getFilename();
        
        // Add folder path if provided
        if (payload.getFolderPath() != null && !payload.getFolderPath().isEmpty()) {
            // Normalize folder path - remove leading slash and ensure no double slashes
            String folderPath = payload.getFolderPath();
            if (folderPath.startsWith("/")) {
                folderPath = folderPath.substring(1);
            }
            if (folderPath.endsWith("/")) {
                folderPath = folderPath.substring(0, folderPath.length() - 1);
            }
            
            if (!folderPath.isEmpty()) {
                return folderPath + "/" + uuid + "_" + filename;
            }
        }
        
        return uuid + "_" + filename;
    }

    private String generatePublicUrl(String fileKey) {
        if (r2Config.getCustomDomain() != null && !r2Config.getCustomDomain().isEmpty()) {
            return String.format("https://%s/%s", r2Config.getCustomDomain(), fileKey);
        }
        
        // Default R2 public URL format
        String publicUrl = String.format("https://pub-%s.r2.dev/%s", r2Config.getAccountId(), fileKey);
        log.info("Generated public URL: {} for file key: {} using account ID: {}", publicUrl, fileKey, r2Config.getAccountId());
        return publicUrl;
    }

    private String generatePrivateUrl(String fileKey) {
        // For private access, we'll generate a presigned URL
        return generatePresignedUrl(fileKey);
    }

    private String generatePresignedUrl(String fileKey) {
        try {
            if (s3Presigner == null) {
                return "#";
            }

            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(fileKey)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            return "#";
        }
    }

    private String extractFilenameFromKey(String key) {
        if (key.contains("/")) {
            key = key.substring(key.lastIndexOf("/") + 1);
        }
        if (key.contains("_")) {
            // Remove UUID prefix if present
            int underscoreIndex = key.indexOf("_");
            if (underscoreIndex > 0) {
                return key.substring(underscoreIndex + 1);
            }
        }
        return key;
    }

    private FileMetadata createDefaultMetadata() {
        return FileMetadata.builder()
            .filename("unknown")
            .fileSize(0L)
            .mimeType("application/octet-stream")
            .lastModified(null)
            .build();
    }

    private String generateFileHash(FilePayload payload) {
        // Simple hash based on filename and size
        return String.valueOf((payload.getFilename() + payload.getFileSize()).hashCode());
    }
}