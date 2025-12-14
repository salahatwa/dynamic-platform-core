package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class TranslationAppRequest {
    
    @NotBlank(message = "App name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Default language is required")
    private String defaultLanguage;
    
    @NotNull(message = "Supported languages are required")
    private Set<String> supportedLanguages;
    
    private Boolean active = true;
}
