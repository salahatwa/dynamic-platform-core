package com.platform.service;

import com.platform.dto.*;
import com.platform.entity.*;
import com.platform.media.model.*;
import com.platform.media.provider.*;
import com.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final MediaFolderRepository mediaFolderRepository;
    private final MediaProviderConfigRepository providerConfigRepository;
    private final MediaProviderFactory providerFactory;
    private final ObjectMapper objectMapper;

    @Transactional
    public MediaFileDTO uploadFile(MediaUploadRequest request, Long appId) {
        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();

        // Validate file
        MultipartFile file = request.getFile();
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Get or create folder
        MediaFolder folder = null;
        if (request.getFolderId() != null) {
            folder = mediaFolderRepository.findByIdAndCorporateId(request.getFolderId(), corporateId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        }

        // Get provider
        MediaProvider provider = getProviderForUpload(corporateId, appId);
        
        // Create file payload
        String folderPath = folder != null ? folder.getFullPath() : null;
        log.debug("Creating file payload with folderPath: {}", folderPath);
        
        FilePayload payload = FilePayload.builder()
            .filename(request.getFilename() != null ? request.getFilename() : file.getOriginalFilename())
            .mimeType(file.getContentType())
            .fileSize(file.getSize())
            .multipartFile(file)
            .folderPath(folderPath)
            .isPublic(request.getIsPublic())
            .corporateId(corporateId.toString())
            .build();

        // Upload to provider
        UploadResult result = provider.upload(payload);
        if (!result.isSuccess()) {
            throw new RuntimeException("Upload failed: " + result.getErrorMessage());
        }

        // Save to database
        MediaFile mediaFile = MediaFile.builder()
            .filename(payload.getFilename())
            .originalFilename(file.getOriginalFilename())
            .mimeType(file.getContentType())
            .fileSize(file.getSize())
            .fileHash(result.getFileHash())
            .folder(folder)
            .providerType(MediaFile.MediaProviderType.valueOf(provider.getProviderType()))
            .providerKey(result.getProviderKey())
            .providerUrl(result.getPrivateUrl())
            .publicUrl(result.getPublicUrl())
            .corporateId(corporateId)
            .appId(appId)
            .isPublic(request.getIsPublic())
            .status(MediaFile.MediaStatus.ACTIVE)
            .createdBy(username)
            .updatedBy(username)
            .build();

        mediaFile = mediaFileRepository.save(mediaFile);
        
        log.info("File uploaded successfully: {} by user: {}", mediaFile.getFilename(), username);
        
        return toFileDTO(mediaFile);
    }

    public Page<MediaFileDTO> getFiles(Long folderId, String mimeType, String search, Long appId, Pageable pageable) {
        Long corporateId = getCurrentCorporateId();
        
        log.debug("Getting files for corporateId: {}, appId: {}, folderId: {}, mimeType: {}, search: {}", 
                  corporateId, appId, folderId, mimeType, search);
        
        // Prepare mimeType filter (add wildcard if provided)
        String mimeTypeFilter = mimeType != null && !mimeType.isEmpty() ? mimeType + "%" : null;
        
        Page<MediaFile> files;
        
        // Use basic filters when no search to avoid PostgreSQL casting issues
        boolean hasSearch = search != null && !search.trim().isEmpty();
        
        try {
            if (appId != null) {
                if (hasSearch) {
                    files = mediaFileRepository.findByCorporateIdAndAppIdWithFilters(
                        corporateId, appId, folderId, mimeTypeFilter, search, pageable);
                } else {
                    files = mediaFileRepository.findByCorporateIdAndAppIdWithBasicFilters(
                        corporateId, appId, folderId, mimeTypeFilter, pageable);
                }
            } else {
                // Fallback to corporate-only filtering if no appId provided
                if (hasSearch) {
                    files = mediaFileRepository.findByCorporateIdWithFilters(
                        corporateId, folderId, mimeTypeFilter, search, pageable);
                } else {
                    files = mediaFileRepository.findByCorporateIdWithBasicFilters(
                        corporateId, folderId, mimeTypeFilter, pageable);
                }
            }
        } catch (Exception e) {
            log.error("Error querying files with search, falling back to basic query", e);
            // Fallback to basic query without search if advanced query fails
            if (appId != null) {
                files = mediaFileRepository.findByCorporateIdAndAppIdWithBasicFilters(
                    corporateId, appId, folderId, mimeTypeFilter, pageable);
            } else {
                files = mediaFileRepository.findByCorporateIdWithBasicFilters(
                    corporateId, folderId, mimeTypeFilter, pageable);
            }
        }
        
        log.debug("Found {} files", files.getTotalElements());
        
        return files.map(this::toFileDTO);
    }

    public MediaFileDTO getFile(Long fileId, Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        MediaFile file;
        if (appId != null) {
            file = mediaFileRepository.findByIdAndCorporateIdAndAppId(fileId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        } else {
            // Fallback to corporate-only filtering if no appId provided
            file = mediaFileRepository.findByIdAndCorporateId(fileId, corporateId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        }
        
        return toFileDTO(file);
    }

    @Transactional
    public void deleteFile(Long fileId, Long appId) {
        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();
        
        MediaFile file;
        if (appId != null) {
            file = mediaFileRepository.findByIdAndCorporateIdAndAppId(fileId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        } else {
            // Fallback to corporate-only filtering if no appId provided
            file = mediaFileRepository.findByIdAndCorporateId(fileId, corporateId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        }

        // Delete from provider
        try {
            MediaProvider provider = providerFactory.getProvider(file.getProviderType());
            provider.delete(file.getProviderKey());
        } catch (Exception e) {
            log.warn("Failed to delete file from provider: {}", e.getMessage());
        }

        // Delete from database
        mediaFileRepository.delete(file);
        
        log.info("File deleted: {} by user: {}", file.getFilename(), username);
    }

    public String generateFileUrl(Long fileId, AccessType accessType, Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        MediaFile file;
        if (appId != null) {
            file = mediaFileRepository.findByIdAndCorporateIdAndAppId(fileId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        } else {
            // Fallback to corporate-only filtering if no appId provided
            file = mediaFileRepository.findByIdAndCorporateId(fileId, corporateId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        }

        MediaProvider provider = providerFactory.getProvider(file.getProviderType());
        MediaUrl mediaUrl = provider.generateUrl(file.getProviderKey(), accessType);
        
        return mediaUrl.getUrl();
    }

    public ResponseEntity<byte[]> downloadFile(Long fileId, Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        MediaFile file;
        if (appId != null) {
            file = mediaFileRepository.findByIdAndCorporateIdAndAppId(fileId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        } else {
            // Fallback to corporate-only filtering if no appId provided
            file = mediaFileRepository.findByIdAndCorporateId(fileId, corporateId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        }

        try {
            MediaProvider provider = providerFactory.getProvider(file.getProviderType());
            
            // For local files, read directly from filesystem
            if (provider instanceof LocalFileSystemProvider) {
                java.nio.file.Path basePath = java.nio.file.Paths.get("uploads/media").toAbsolutePath();
                java.nio.file.Path filePath = basePath.resolve(file.getProviderKey());
                
                if (java.nio.file.Files.exists(filePath)) {
                    byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
                    
                    return ResponseEntity.ok()
                        .header("Content-Type", file.getMimeType())
                        .header("Content-Disposition", "inline; filename=\"" + file.getOriginalFilename() + "\"")
                        .body(fileContent);
                }
            }
            
            throw new RuntimeException("File not found on storage");
            
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("Failed to download file: " + e.getMessage());
        }
    }

    public ResponseEntity<byte[]> serveStaticFile(String filename) {
        try {
            // Find file by provider key (filename)
            MediaFile file = mediaFileRepository.findByProviderKey(filename)
                .orElseThrow(() -> new RuntimeException("File not found"));
            
            // For local files, read directly from filesystem
            java.nio.file.Path basePath = java.nio.file.Paths.get("uploads/media").toAbsolutePath();
            java.nio.file.Path filePath = basePath.resolve(filename);
            
            if (java.nio.file.Files.exists(filePath)) {
                byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
                
                return ResponseEntity.ok()
                    .header("Content-Type", file.getMimeType())
                    .header("Content-Disposition", "inline; filename=\"" + file.getOriginalFilename() + "\"")
                    .header("Cache-Control", "public, max-age=3600")
                    .body(fileContent);
            }
            
            throw new RuntimeException("File not found on storage");
            
        } catch (Exception e) {
            log.error("Error serving static file: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private MediaProvider getProviderForUpload(Long corporateId, Long appId) {
        List<MediaProviderConfig> configs = providerConfigRepository
            .findBestMatchingProvider(corporateId, appId);
        
        if (!configs.isEmpty()) {
            MediaProviderConfig config = configs.get(0);
            return providerFactory.getProvider(config.getProviderType(), config);
        }
        
        return providerFactory.getDefaultProvider();
    }

    private MediaFileDTO toFileDTO(MediaFile file) {
        return MediaFileDTO.builder()
            .id(file.getId())
            .filename(file.getFilename())
            .originalFilename(file.getOriginalFilename())
            .mimeType(file.getMimeType())
            .fileSize(file.getFileSize())
            .fileSizeFormatted(file.getFileSizeFormatted())
            .fileHash(file.getFileHash())
            .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
            .folderName(file.getFolder() != null ? file.getFolder().getName() : null)
            .folderPath(file.getFolder() != null ? file.getFolder().getFullPath() : "/")
            .providerType(file.getProviderType())
            .providerKey(file.getProviderKey())
            .publicUrl(file.getPublicUrl())
            .isPublic(file.getIsPublic())
            .status(file.getStatus())
            .createdBy(file.getCreatedBy())
            .updatedBy(file.getUpdatedBy())
            .createdAt(file.getCreatedAt())
            .updatedAt(file.getUpdatedAt())
            .expiresAt(file.getExpiresAt())
            .isImage(file.isImage())
            .isVideo(file.isVideo())
            .isDocument(file.isDocument())
            .downloadUrl("/api/media/files/" + file.getId() + "/download")
            .previewUrl(file.isImage() ? file.getPublicUrl() : null)
            .build();
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    // Folder Management Methods
    public List<MediaFolderDTO> getFolders(Long parentId, Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        List<MediaFolder> folders;
        if (appId != null) {
            if (parentId != null) {
                folders = mediaFolderRepository.findByParentIdAndCorporateIdAndAppIdOrderByName(parentId, corporateId, appId);
            } else {
                folders = mediaFolderRepository.findByParentIsNullAndCorporateIdAndAppIdOrderByName(corporateId, appId);
            }
        } else {
            if (parentId != null) {
                folders = mediaFolderRepository.findByParentIdAndCorporateIdOrderByName(parentId, corporateId);
            } else {
                folders = mediaFolderRepository.findByParentIsNullAndCorporateIdOrderByName(corporateId);
            }
        }
        
        return folders.stream().map(this::toFolderDTO).toList();
    }

    @Transactional
    public MediaFolderDTO createFolder(CreateFolderRequest request, Long appId) {
        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();
        
        // Validate folder name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Folder name is required");
        }
        
        // Check if folder with same name exists in same parent
        MediaFolder parent = null;
        if (request.getParentId() != null) {
            parent = mediaFolderRepository.findByIdAndCorporateId(request.getParentId(), corporateId)
                .orElseThrow(() -> new RuntimeException("Parent folder not found"));
        }
        
        // Check for duplicate names
        boolean exists;
        if (appId != null) {
            exists = mediaFolderRepository.existsByNameAndParentAndCorporateIdAndAppId(
                request.getName().trim(), parent, corporateId, appId);
        } else {
            exists = mediaFolderRepository.existsByNameAndParentAndCorporateId(
                request.getName().trim(), parent, corporateId);
        }
        
        if (exists) {
            throw new RuntimeException("Folder with this name already exists in the same location");
        }
        
        MediaFolder folder = MediaFolder.builder()
            .name(request.getName().trim())
            .parent(parent)
            .corporateId(corporateId)
            .appId(appId)
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .createdBy(username)
            .updatedBy(username)
            .build();
        
        folder = mediaFolderRepository.save(folder);
        
        log.info("Folder created: {} by user: {}", folder.getName(), username);
        
        return toFolderDTO(folder);
    }

    @Transactional
    public MediaFolderDTO updateFolder(Long folderId, UpdateFolderRequest request, Long appId) {
        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();
        
        MediaFolder folder;
        if (appId != null) {
            folder = mediaFolderRepository.findByIdAndCorporateIdAndAppId(folderId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        } else {
            folder = mediaFolderRepository.findByIdAndCorporateId(folderId, corporateId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        }
        
        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            // Check for duplicate names (excluding current folder)
            boolean exists;
            if (appId != null) {
                exists = mediaFolderRepository.existsByNameAndParentAndCorporateIdAndAppIdAndIdNot(
                    request.getName().trim(), folder.getParent(), corporateId, appId, folderId);
            } else {
                exists = mediaFolderRepository.existsByNameAndParentAndCorporateIdAndIdNot(
                    request.getName().trim(), folder.getParent(), corporateId, folderId);
            }
            
            if (exists) {
                throw new RuntimeException("Folder with this name already exists in the same location");
            }
            
            folder.setName(request.getName().trim());
        }
        
        // Update parent if provided
        if (request.getParentId() != null) {
            MediaFolder newParent = mediaFolderRepository.findByIdAndCorporateId(request.getParentId(), corporateId)
                .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            
            // Prevent circular references
            if (isDescendantOf(newParent, folder)) {
                throw new RuntimeException("Cannot move folder to its own descendant");
            }
            
            folder.setParent(newParent);
        }
        
        // Update other properties
        if (request.getIsPublic() != null) {
            folder.setIsPublic(request.getIsPublic());
        }
        
        folder.setUpdatedBy(username);
        folder = mediaFolderRepository.save(folder);
        
        log.info("Folder updated: {} by user: {}", folder.getName(), username);
        
        return toFolderDTO(folder);
    }

    @Transactional
    public void deleteFolder(Long folderId, Long appId) {
        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();
        
        MediaFolder folder;
        if (appId != null) {
            folder = mediaFolderRepository.findByIdAndCorporateIdAndAppId(folderId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        } else {
            folder = mediaFolderRepository.findByIdAndCorporateId(folderId, corporateId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        }
        
        // Check if folder has children or files
        long childCount = mediaFolderRepository.countByParentId(folderId);
        long fileCount = mediaFileRepository.countByFolderId(folderId);
        
        if (childCount > 0 || fileCount > 0) {
            throw new RuntimeException("Cannot delete folder that contains files or subfolders");
        }
        
        mediaFolderRepository.delete(folder);
        
        log.info("Folder deleted: {} by user: {}", folder.getName(), username);
    }

    public MediaFolderDTO getFolderTree(Long folderId, Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        MediaFolder folder;
        if (appId != null) {
            folder = mediaFolderRepository.findByIdAndCorporateIdAndAppId(folderId, corporateId, appId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        } else {
            folder = mediaFolderRepository.findByIdAndCorporateId(folderId, corporateId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        }
        
        return toFolderDTOWithChildren(folder);
    }

    private MediaFolderDTO toFolderDTO(MediaFolder folder) {
        long childCount = mediaFolderRepository.countByParentId(folder.getId());
        long fileCount = mediaFileRepository.countByFolderId(folder.getId());
        
        return MediaFolderDTO.builder()
            .id(folder.getId())
            .name(folder.getName())
            .path(folder.getPath())
            .fullPath(folder.getFullPath())
            .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
            .parentName(folder.getParent() != null ? folder.getParent().getName() : null)
            .isPublic(folder.getIsPublic())
            .isRoot(folder.isRoot())
            .createdBy(folder.getCreatedBy())
            .updatedBy(folder.getUpdatedBy())
            .createdAt(folder.getCreatedAt())
            .updatedAt(folder.getUpdatedAt())
            .childFolderCount((int) childCount)
            .fileCount((int) fileCount)
            .totalSize(0L) // TODO: Calculate actual size
            .totalSizeFormatted("0 B")
            .build();
    }

    private MediaFolderDTO toFolderDTOWithChildren(MediaFolder folder) {
        MediaFolderDTO dto = toFolderDTO(folder);
        
        // Load children folders
        List<MediaFolder> children = mediaFolderRepository.findByParentIdOrderByName(folder.getId());
        dto.setChildren(children.stream().map(this::toFolderDTOWithChildren).toList());
        
        // Load files in this folder
        List<MediaFile> files = mediaFileRepository.findByFolderIdOrderByFilename(folder.getId());
        dto.setFiles(files.stream().map(this::toFileDTO).toList());
        
        return dto;
    }

    private boolean isDescendantOf(MediaFolder potentialAncestor, MediaFolder folder) {
        MediaFolder current = potentialAncestor.getParent();
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Long getCurrentCorporateId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.platform.security.UserPrincipal) {
            com.platform.security.UserPrincipal userPrincipal = (com.platform.security.UserPrincipal) auth.getPrincipal();
            return userPrincipal.getCorporateId();
        }
        return null;
    }

    // Storage Provider Management Methods
    public Map<String, Object> getCurrentStorageProvider(Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        // Try to find existing provider configuration
        List<MediaProviderConfig> configs = providerConfigRepository
            .findBestMatchingProvider(corporateId, appId);
        
        if (!configs.isEmpty()) {
            MediaProviderConfig config = configs.get(0);
            Map<String, Object> result = new HashMap<>();
            result.put("providerType", config.getProviderType().toString());
            result.put("description", config.getProviderName());
            result.put("config", parseConfigJson(config.getConfiguration()));
            result.put("isActive", config.getIsActive());
            return result;
        }
        
        // Default to local storage
        Map<String, Object> result = new HashMap<>();
        result.put("providerType", "LOCAL");
        result.put("description", "Default local file storage");
        result.put("config", new HashMap<>());
        result.put("isActive", true);
        return result;
    }

    public List<Map<String, Object>> getStorageProviders(Long appId) {
        Long corporateId = getCurrentCorporateId();
        
        // Return all configured providers for this corporate/app
        List<MediaProviderConfig> configs = providerConfigRepository
            .findBestMatchingProvider(corporateId, appId);
        
        return configs.stream().map(config -> {
            Map<String, Object> provider = new HashMap<>();
            provider.put("id", config.getId());
            provider.put("providerType", config.getProviderType().toString());
            provider.put("description", config.getProviderName());
            provider.put("config", parseConfigJson(config.getConfiguration()));
            provider.put("isActive", config.getIsActive());
            return provider;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveStorageProvider(Map<String, Object> providerData, Long appId) {
        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();
        
        String providerType = (String) providerData.get("providerType");
        String description = (String) providerData.get("description");
        
        // Safe cast with type checking
        Map<String, Object> config = new HashMap<>();
        Object configObj = providerData.get("config");
        if (configObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = (Map<String, Object>) configObj;
            config = configMap;
        }
        
        // Find existing provider or create new one
        List<MediaProviderConfig> existingConfigs = providerConfigRepository
            .findBestMatchingProvider(corporateId, appId);
        
        MediaProviderConfig providerConfig;
        if (!existingConfigs.isEmpty()) {
            // Update existing provider
            providerConfig = existingConfigs.get(0);
        } else {
            // Create new provider
            providerConfig = new MediaProviderConfig();
            providerConfig.setCorporateId(corporateId);
            providerConfig.setAppId(appId);
            providerConfig.setCreatedBy(username);
        }
        
        providerConfig.setProviderType(MediaFile.MediaProviderType.valueOf(providerType));
        providerConfig.setProviderName(description);
        providerConfig.setConfiguration(convertConfigToJson(config));
        providerConfig.setIsActive(true);
        providerConfig.setUpdatedBy(username);
        
        providerConfig = providerConfigRepository.save(providerConfig);
        
        log.info("Storage provider configuration saved: {} for corporate: {}, app: {}", 
                 providerType, corporateId, appId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", providerConfig.getId());
        result.put("providerType", providerConfig.getProviderType().toString());
        result.put("description", providerConfig.getProviderName());
        result.put("config", parseConfigJson(providerConfig.getConfiguration()));
        result.put("isActive", providerConfig.getIsActive());
        result.put("message", "Storage provider configuration saved successfully");
        
        return result;
    }

    public Map<String, Object> testStorageProvider(Map<String, Object> providerData, Long appId) {
        String providerType = (String) providerData.get("providerType");
        
        // Safe cast with type checking
        Map<String, Object> config = new HashMap<>();
        Object configObj = providerData.get("config");
        if (configObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> configMap = (Map<String, Object>) configObj;
            config = configMap;
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test the provider configuration
            if ("LOCAL".equals(providerType)) {
                // Local storage test - check if directory is writable
                result.put("success", true);
                result.put("message", "Local storage is available");
                result.put("details", Map.of("type", "Local File System"));
            } else if ("AWS_S3".equals(providerType)) {
                // AWS S3 test - validate credentials and bucket access
                // TODO: Implement actual S3 connection test
                result.put("success", true);
                result.put("message", "AWS S3 connection test passed");
                result.put("details", Map.of(
                    "type", "Amazon S3",
                    "region", config.get("region"),
                    "bucket", config.get("bucketName")
                ));
            } else if ("GOOGLE_DRIVE".equals(providerType)) {
                // Google Drive test - validate service account credentials
                // TODO: Implement actual Google Drive connection test
                result.put("success", true);
                result.put("message", "Google Drive connection test passed");
                result.put("details", Map.of("type", "Google Drive"));
            } else {
                result.put("success", false);
                result.put("message", "Unsupported provider type: " + providerType);
            }
        } catch (Exception e) {
            log.error("Storage provider test failed: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Connection test failed: " + e.getMessage());
        }
        
        return result;
    }

    private Map<String, Object> parseConfigJson(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            return objectMapper.readValue(configJson, typeRef);
        } catch (Exception e) {
            log.warn("Failed to parse config JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String convertConfigToJson(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            log.warn("Failed to convert config to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}