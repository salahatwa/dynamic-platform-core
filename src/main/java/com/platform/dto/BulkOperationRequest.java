package com.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkOperationRequest {
    
    @NotEmpty(message = "Template IDs cannot be empty")
    private List<Long> templateIds;
    
    @NotNull(message = "Target folder ID is required")
    private Long targetFolderId;
    
    private BulkOperationType operation;
    
    public enum BulkOperationType {
        MOVE, COPY, DELETE
    }
}