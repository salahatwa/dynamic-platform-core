package com.platform.dto;

import com.platform.entity.MediaFile;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaFileDTO {
    private Long id;
    private String filename;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String fileHash;
    private Long folderId;
    private String folderName;
    private String folderPath;
    private MediaFile.MediaProviderType providerType;
    private String providerKey;
    private String publicUrl;
    private Boolean isPublic;
    private MediaFile.MediaStatus status;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    // File type helpers
    private Boolean isImage;
    private Boolean isVideo;
    private Boolean isDocument;
    
    // URLs
    private String downloadUrl;
    private String previewUrl;
    private String thumbnailUrl;
}