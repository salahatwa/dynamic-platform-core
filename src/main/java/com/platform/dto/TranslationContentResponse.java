package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationContentResponse {
    private Long id;
    private String keyName;
    private String defaultValue;
    private String description;
    private String category;
    private Map<String, String> translations; // languageCode -> translatedValue
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}