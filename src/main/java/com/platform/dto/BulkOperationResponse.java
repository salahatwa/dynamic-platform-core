package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkOperationResponse {
    private int totalItems;
    private int successCount;
    private int failureCount;
    private List<OperationResult> results;
    private String message;
    
    @Data
    @Builder
    public static class OperationResult {
        private Long templateId;
        private String templateName;
        private boolean success;
        private String errorMessage;
    }
}