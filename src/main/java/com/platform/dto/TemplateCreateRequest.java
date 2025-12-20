package com.platform.dto;

import com.platform.enums.TemplateType;
import com.platform.enums.PageOrientation;
import lombok.Data;

@Data
public class TemplateCreateRequest {
    private String name;
    private TemplateType type;
    private String htmlContent;
    private String cssStyles;
    private String subject;
    private Long folderId;
    private PageOrientation pageOrientation;
}