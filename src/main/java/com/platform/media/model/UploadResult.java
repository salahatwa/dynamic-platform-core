package com.platform.media.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class UploadResult {
    private String providerKey;
    private String publicUrl;
    private String privateUrl;
    private Long fileSize;
    private String mimeType;
    private String fileHash;
    private boolean success;
    private String errorMessage;
}