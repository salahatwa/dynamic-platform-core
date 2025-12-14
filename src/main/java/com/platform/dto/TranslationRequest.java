package com.platform.dto;

import com.platform.enums.TranslationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TranslationRequest {
    
    @NotNull(message = "Key ID is required")
    private Long keyId;
    
    @NotBlank(message = "Language is required")
    private String language;
    
    @NotBlank(message = "Translation value is required")
    private String value;
    
    private TranslationStatus status = TranslationStatus.PUBLISHED;
}
