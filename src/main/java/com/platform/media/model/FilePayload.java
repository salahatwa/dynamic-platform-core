package com.platform.media.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Data
@Builder
public class FilePayload {
    private String filename;
    private String mimeType;
    private Long fileSize;
    private InputStream inputStream;
    private MultipartFile multipartFile;
    private String folderPath;
    private boolean isPublic;
    private String corporateId;
    private String appId;
}