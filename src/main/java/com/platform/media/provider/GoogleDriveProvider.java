package com.platform.media.provider;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.platform.entity.MediaProviderConfig;
import com.platform.media.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class GoogleDriveProvider implements MediaProvider {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Dynamic Platform";
    
    private MediaProviderConfig config;
    private GoogleDriveConfig driveConfig;
    private Drive driveService;

    public GoogleDriveProvider() {
        // Default constructor for Spring
    }

    public GoogleDriveProvider(MediaProviderConfig config) {
        this.config = config;
        this.driveConfig = parseConfig(config);
        this.driveService = initializeDriveService();
    }

    @Override
    public UploadResult upload(FilePayload payload) {
        try {
            if (driveService == null) {
                return UploadResult.builder()
                    .success(false)
                    .errorMessage("Google Drive service not initialized. Please check configuration.")
                    .build();
            }

            // Create file metadata
            File fileMetadata = new File();
            fileMetadata.setName(payload.getFilename());
            
            // Set parent folder if specified
            if (driveConfig.getFolderId() != null && !driveConfig.getFolderId().isEmpty()) {
                fileMetadata.setParents(Collections.singletonList(driveConfig.getFolderId()));
            }

            // Create file content
            InputStreamContent mediaContent = new InputStreamContent(
                payload.getMimeType(),
                payload.getMultipartFile().getInputStream()
            );
            mediaContent.setLength(payload.getFileSize());

            // Upload file
            File uploadedFile = driveService.files()
                .create(fileMetadata, mediaContent)
                .setFields("id,name,size,mimeType,webViewLink,webContentLink")
                .execute();

            // Set permissions if sharing publicly
            String publicUrl = null;
            if (driveConfig.isSharePublicly()) {
                Permission permission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
                
                driveService.permissions()
                    .create(uploadedFile.getId(), permission)
                    .execute();
                
                publicUrl = "https://drive.google.com/file/d/" + uploadedFile.getId() + "/view";
            }

            log.info("File uploaded to Google Drive: {} (ID: {})", uploadedFile.getName(), uploadedFile.getId());

            return UploadResult.builder()
                .success(true)
                .providerKey(uploadedFile.getId())
                .publicUrl(publicUrl)
                .privateUrl(uploadedFile.getWebViewLink())
                .fileHash(generateFileHash(payload))
                .build();

        } catch (Exception e) {
            log.error("Failed to upload file to Google Drive: {}", e.getMessage(), e);
            return UploadResult.builder()
                .success(false)
                .errorMessage("Upload failed: " + e.getMessage())
                .build();
        }
    }

    @Override
    public void delete(String providerKey) {
        try {
            if (driveService == null) {
                log.warn("Google Drive service not initialized, cannot delete file: {}", providerKey);
                return;
            }

            driveService.files().delete(providerKey).execute();
            log.info("File deleted from Google Drive: {}", providerKey);

        } catch (Exception e) {
            log.error("Failed to delete file from Google Drive: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public MediaUrl generateUrl(String providerKey, AccessType accessType) {
        try {
            if (driveService == null) {
                return MediaUrl.builder()
                    .url("#")
                    .expiresAt(null)
                    .isTemporary(false)
                    .build();
            }

            File file = driveService.files()
                .get(providerKey)
                .setFields("webViewLink,webContentLink")
                .execute();

            String url = accessType == AccessType.PRIVATE ? 
                file.getWebContentLink() : file.getWebViewLink();

            return MediaUrl.builder()
                .url(url != null ? url : "https://drive.google.com/file/d/" + providerKey + "/view")
                .expiresAt(null) // Google Drive links don't expire
                .isTemporary(false)
                .build();

        } catch (Exception e) {
            log.error("Failed to generate URL for Google Drive file: {}", e.getMessage());
            return MediaUrl.builder()
                .url("https://drive.google.com/file/d/" + providerKey + "/view")
                .expiresAt(null)
                .isTemporary(false)
                .build();
        }
    }

    @Override
    public boolean exists(String providerKey) {
        try {
            if (driveService == null) {
                return false;
            }

            driveService.files()
                .get(providerKey)
                .setFields("id")
                .execute();
            
            return true;

        } catch (Exception e) {
            log.debug("File does not exist in Google Drive: {}", providerKey);
            return false;
        }
    }

    @Override
    public FileMetadata getMetadata(String providerKey) {
        try {
            if (driveService == null) {
                return createDefaultMetadata();
            }

            File file = driveService.files()
                .get(providerKey)
                .setFields("name,size,mimeType,modifiedTime")
                .execute();

            return FileMetadata.builder()
                .filename(file.getName())
                .fileSize(file.getSize() != null ? file.getSize() : 0L)
                .mimeType(file.getMimeType())
                .lastModified(file.getModifiedTime() != null ? 
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(file.getModifiedTime().getValue()),
                        ZoneId.systemDefault()
                    ) : null)
                .build();

        } catch (Exception e) {
            log.error("Failed to get metadata for Google Drive file: {}", e.getMessage());
            return createDefaultMetadata();
        }
    }

    @Override
    public String getProviderType() {
        return "GOOGLE_DRIVE";
    }

    @Override
    public boolean validateConfiguration() {
        try {
            if (config == null) {
                return false;
            }

            GoogleDriveConfig testConfig = parseConfig(config);
            if (testConfig.getServiceAccountJson() == null || testConfig.getServiceAccountJson().isEmpty()) {
                return false;
            }

            // Try to initialize the service
            Drive testService = createDriveService(testConfig);
            
            // Test by getting user info
            testService.about().get().setFields("user").execute();
            
            return true;

        } catch (Exception e) {
            log.error("Google Drive configuration validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Create a configured instance of GoogleDriveProvider
     */
    public static GoogleDriveProvider create(MediaProviderConfig config) {
        return new GoogleDriveProvider(config);
    }

    // Private helper methods

    private GoogleDriveConfig parseConfig(MediaProviderConfig config) {
        if (config == null || config.getConfiguration() == null) {
            return GoogleDriveConfig.builder()
                .applicationName(APPLICATION_NAME)
                .sharePublicly(false)
                .build();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = mapper.readValue(config.getConfiguration(), Map.class);
            return GoogleDriveConfig.fromMap(configMap);
        } catch (Exception e) {
            log.error("Failed to parse Google Drive configuration: {}", e.getMessage());
            return GoogleDriveConfig.builder()
                .applicationName(APPLICATION_NAME)
                .sharePublicly(false)
                .build();
        }
    }

    private Drive initializeDriveService() {
        try {
            if (driveConfig == null) {
                log.debug("Google Drive configuration is null, service not initialized");
                return null;
            }
            if (driveConfig.getServiceAccountJson() == null || driveConfig.getServiceAccountJson().isEmpty()) {
                log.debug("Google Drive service account JSON is empty, service not initialized");
                return null;
            }
            return createDriveService(driveConfig);
        } catch (Exception e) {
            log.error("Failed to initialize Google Drive service: {}", e.getMessage());
            return null;
        }
    }

    private Drive createDriveService(GoogleDriveConfig config) throws IOException, GeneralSecurityException {
        if (config.getServiceAccountJson() == null || config.getServiceAccountJson().isEmpty()) {
            throw new IllegalArgumentException("Service account JSON is required");
        }

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        ServiceAccountCredentials credentials = (ServiceAccountCredentials) ServiceAccountCredentials
            .fromStream(new ByteArrayInputStream(config.getServiceAccountJson().getBytes()))
            .createScoped(Collections.singleton(DriveScopes.DRIVE));

        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
            .setApplicationName(config.getApplicationName())
            .build();
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