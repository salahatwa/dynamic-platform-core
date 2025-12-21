package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateFolderRequest {
    
    @NotBlank(message = "Folder name is required")
    private String name;
    
    private Long parentId;
    
    @NotNull(message = "Application ID is required")
    private Long applicationId;
    
    private Integer sortOrder;
    
    private Boolean active = true;
    
    private String description;
    
    private String imageUrl;
}
