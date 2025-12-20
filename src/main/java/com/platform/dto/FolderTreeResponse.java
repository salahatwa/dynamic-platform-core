package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FolderTreeResponse {
    private FolderTreeNode rootFolder;
    private Long totalFolders;
    
    @Data
    @Builder
    public static class FolderTreeNode {
        private Long id;
        private String name;
        private Long parentId;
        private Long applicationId;
        private String path;
        private Integer level;
        private Integer sortOrder;
        private List<FolderTreeNode> children;
        private boolean isExpanded;
        private boolean isLoading;
        private boolean hasChildren;
        private Long templateCount;
        private Long subfolderCount;
        private FolderPermissions permissions;
    }
    
    @Data
    @Builder
    public static class FolderPermissions {
        private boolean canView;
        private boolean canCreate;
        private boolean canEdit;
        private boolean canDelete;
    }
}