package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TranslationKeyResponse {
    private Long id;
    private Long appId;
    private String keyName;
    private String description;
    private String context;
    private Map<String, TranslationValueResponse> translations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
