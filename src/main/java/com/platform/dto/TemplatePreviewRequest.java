package com.platform.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TemplatePreviewRequest {
    private Long templateId;
    private Map<String, Object> parameters;
    private Integer pageNumber; // null for all pages
    private Boolean generateThumbnails;
}
