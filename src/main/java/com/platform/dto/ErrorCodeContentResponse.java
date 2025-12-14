package com.platform.dto;

import com.platform.entity.ErrorCode;
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
public class ErrorCodeContentResponse {
    private Long id;
    private String errorCode;
    private ErrorCode.ErrorSeverity severity;
    private ErrorCode.ErrorStatus status;
    private Integer httpStatusCode;
    private Boolean isPublic;
    private Boolean isRetryable;
    private String defaultMessage;
    private String technicalDetails;
    private String resolutionSteps;
    private String documentationUrl;
    private String moduleName;
    private Long categoryId;
    private String categoryName;
    private Map<String, TranslationData> translations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranslationData {
        private String message;
        private String technicalDetails;
        private String resolutionSteps;
    }
}