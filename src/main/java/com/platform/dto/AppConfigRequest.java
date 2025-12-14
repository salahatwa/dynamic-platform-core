package com.platform.dto;

import com.platform.entity.AppConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppConfigRequest {
    
    @NotBlank(message = "Config key is required")
    private String configKey;
    
    @NotBlank(message = "Config name is required")
    private String configName;
    
    private String description;
    
    @NotNull(message = "Config type is required")
    private AppConfig.ConfigType configType;
    
    private String configValue;
    
    private String defaultValue;
    
    private String enumValues; // JSON array for ENUM type
    
    private String validationRules; // JSON object
    
    private Boolean isPublic = false;
    
    private Boolean isRequired = false;
    
    private Integer displayOrder = 0;
    
    private Long groupId;
    
    @NotBlank(message = "App name is required")
    private String appName;
    
    private Boolean active = true;
}
