package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class TranslationAppResponse {
    private Long id;
    private String name;
    private String description;
    private String apiKey;
    private String defaultLanguage;
    private Set<String> supportedLanguages;
    private Boolean active;
    private Long keysCount;
    private Long translationsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
