package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LovWithTranslationsDTO {
    private Long id;
    private String lovCode;
    private String lovType;
    private String lovValue;
    private String attribute1;
    private String attribute2;
    private String attribute3;
    private Integer displayOrder;
    private Boolean active;
    private Long parentLovId;
    private String translationApp;
    private String translationKey;
    private Map<String, String> descriptions; // Language code -> Description
    private String metadata;
    private Long corporateId;
    private Integer version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
