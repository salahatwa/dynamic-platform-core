package com.platform.media.model;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class FileMetadata {
    private String filename;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private LocalDateTime lastModified;
    private boolean exists;
}