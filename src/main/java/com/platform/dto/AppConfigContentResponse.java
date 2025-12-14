package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigContentResponse {
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private String dataType;
    private Boolean isRequired;
    private Boolean isEncrypted;
    private String defaultValue;
    private String validationRule;
    private String groupName;
    private String groupDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}