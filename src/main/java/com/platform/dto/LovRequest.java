package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LovRequest {
    
    @NotBlank(message = "LOV code is required")
    private String lovCode;
    
    @NotBlank(message = "LOV type is required")
    private String lovType;
    
    private String lovValue;
    
    private String attribute1;
    
    private String attribute2;
    
    private String attribute3;
    
    @NotNull(message = "Display order is required")
    private Integer displayOrder;
    
    @NotNull(message = "Active status is required")
    private Boolean active;
    
    private Long parentLovId;
    
    private String translationApp;
    
    private String translationKey;
    
    private String metadata; // JSON string

    @NotBlank(message = "App name is required")
    private String appName;
}
