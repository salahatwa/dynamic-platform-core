package com.platform.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MediaFolderDTO {
    private Long id;
    private String name;
    private String path;
    private String fullPath;
    private Long parentId;
    private String parentName;
    private Boolean isPublic;
    private Boolean isRoot;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Counts
    private Integer childFolderCount;
    private Integer fileCount;
    private Long totalSize;
    private String totalSizeFormatted;
    
    // Children (for tree view)
    private List<MediaFolderDTO> children;
    private List<MediaFileDTO> files;
}