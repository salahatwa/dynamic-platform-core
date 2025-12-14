package com.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplatePageRequest {
    
    @NotBlank(message = "Page name is required")
    private String name;
    
    private String content;
    
    private Integer pageOrder;
}
