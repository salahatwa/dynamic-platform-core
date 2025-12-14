package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateAttributeRequest {
    
    @NotBlank(message = "Attribute key is required")
    private String attributeKey;
    
    private String attributeValue;
    
    private String attributeType = "STRING";
    
    private String description;
}
