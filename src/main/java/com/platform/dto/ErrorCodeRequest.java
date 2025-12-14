package com.platform.dto;

import com.platform.entity.ErrorCode;
import lombok.Data;
import java.util.Map;

@Data
public class ErrorCodeRequest {
    private String errorCode;
    private Long categoryId;
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
    private Map<String, TranslationData> translations; // language -> translation data

    @Data
    public static class TranslationData {
        private String message;
        private String technicalDetails;
        private String resolutionSteps;
    }
}
