package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TranslationKeyRequest {
    
    @NotNull(message = "App ID is required")
    private Long appId;
    
    @NotBlank(message = "Key name is required")
    private String keyName;
    
    private String description;
    
    private String context;
}
