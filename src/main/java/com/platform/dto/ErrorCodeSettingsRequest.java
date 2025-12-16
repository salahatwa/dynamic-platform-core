package com.platform.dto;

import lombok.Data;

@Data
public class ErrorCodeSettingsRequest {
    private String prefix;
    private Integer sequenceLength;
    private String separator;
    private Boolean isActive;
    
    // Validation constraints
    public boolean isValid() {
        return prefix != null && !prefix.trim().isEmpty() && prefix.length() <= 10 &&
               sequenceLength != null && sequenceLength >= 1 && sequenceLength <= 10;
    }
}