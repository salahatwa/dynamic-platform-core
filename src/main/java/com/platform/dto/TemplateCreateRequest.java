package com.platform.dto;

import com.platform.enums.TemplateType;
import lombok.Data;

import java.util.Map;

@Data
public class TemplateCreateRequest {
    private String name;
    private TemplateType type;
    private String htmlContent;
    private String cssStyles;
    private Map<String, String> customFonts;
    private Map<String, String> parameters;
    private String subject;
    private Long corporateId;
}
