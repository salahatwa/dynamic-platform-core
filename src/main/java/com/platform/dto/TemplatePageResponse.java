package com.platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TemplatePageResponse {
    private Long id;
    private Long templateId;
    private String name;
    private String content;
    private Integer pageOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
