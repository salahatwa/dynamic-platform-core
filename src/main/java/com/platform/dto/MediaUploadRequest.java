package com.platform.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MediaUploadRequest {
    private MultipartFile file;
    private Long folderId;
    private String filename;
    private Boolean isPublic = false;
    private String description;
    private String providerType;
}