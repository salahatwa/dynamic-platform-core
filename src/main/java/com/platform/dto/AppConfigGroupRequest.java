package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppConfigGroupRequest {
    
    @NotBlank(message = "Group key is required")
    private String groupKey;
    
    @NotBlank(message = "Group name is required")
    private String groupName;
    
    private String description;
    
    @NotBlank(message = "App name is required")
    private String appName;
    
    private Integer displayOrder = 0;
    
    private Boolean active = true;
}
