package com.platform.dto;

import com.platform.enums.TranslationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TranslationValueResponse {
    private Long id;
    private String language;
    private String value;
    private TranslationStatus status;
    private LocalDateTime updatedAt;
}
