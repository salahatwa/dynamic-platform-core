package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateFolderRequest {
    
    @NotBlank(message = "Folder name is required")
    private String name;
    
    private Long parentId;
}
