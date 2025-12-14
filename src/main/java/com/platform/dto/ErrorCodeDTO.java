package com.platform.dto;

import com.platform.entity.ErrorCode;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ErrorCodeDTO {
    private Long id;
    private String errorCode;
    private Long categoryId;
    private String categoryName;
    private String appName;
    private String moduleName;
    private ErrorCode.ErrorSeverity severity;
    private ErrorCode.ErrorStatus status;
    private Integer httpStatusCode;
    private Boolean isPublic;
    private Boolean isRetryable;
    private String defaultMessage;
    private String technicalDetails;
    private String resolutionSteps;
    private String documentationUrl;
    private Map<String, TranslationData> translations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @Data
    public static class TranslationData {
        private Long id;
        private String message;
        private String technicalDetails;
        private String resolutionSteps;
    }
}
