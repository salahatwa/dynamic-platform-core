package com.platform.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FolderContentResponse {
    private TemplateFolderResponse currentFolder;
    private Page<TemplateResponse> templates;
    private List<TemplateFolderResponse> subfolders;
    private List<BreadcrumbItem> breadcrumbs;
    private Long totalItems;
    private boolean isLoading;
    
    @Data
    @Builder
    public static class TemplateResponse {
        private Long id;
        private String name;
        private String type;
        private Long folderId;
        private String folderPath;
        private Long applicationId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private TemplatePermissions permissions;
    }
    
    @Data
    @Builder
    public static class TemplatePermissions {
        private boolean canView;
        private boolean canEdit;
        private boolean canDelete;
        private boolean canCopy;
    }
    
    @Data
    @Builder
    public static class BreadcrumbItem {
        private Long id;
        private String name;
        private String path;
    }
}