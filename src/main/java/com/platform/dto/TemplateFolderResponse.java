package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TemplateFolderResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Long applicationId;
    private Long corporateId;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private List<TemplateFolderResponse> children;
    private Long templatesCount;
    private Long subfoldersCount;
    private FolderTreeResponse.FolderPermissions permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
