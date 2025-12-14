package com.platform.dto;

import lombok.Data;

@Data
public class ErrorCodeCategoryRequest {
    private String categoryCode;
    private String categoryName;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
}
