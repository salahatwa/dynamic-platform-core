package com.platform.dto;

import com.platform.enums.TemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateContentResponse {
    private Long id;
    private String name;
    private TemplateType type;
    private String htmlContent;
    private String cssStyles;
    private String customFonts;
    private String parameters;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}