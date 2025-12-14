package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TemplateAttributeResponse {
    private Long id;
    private Long templateId;
    private String attributeKey;
    private String attributeValue;
    private String attributeType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
