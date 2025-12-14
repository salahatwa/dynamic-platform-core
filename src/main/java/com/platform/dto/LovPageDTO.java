package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LovPageDTO {
    private String lovCode;
    private String name;
    private String description;
    private String translationApp;
    private Long valueCount;
    private Boolean active;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
