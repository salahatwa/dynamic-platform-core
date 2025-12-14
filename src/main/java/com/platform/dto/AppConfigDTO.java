package com.platform.dto;

import com.platform.entity.AppConfig;
import lombok.Data;

@Data
public class AppConfigDTO {
    private Long id;
    private String configKey;
    private String configName;
    private String description;
    private AppConfig.ConfigType configType;
    private String configValue;
    private String defaultValue;
    private String enumValues;
    private String validationRules;
    private Boolean isPublic;
    private Boolean isRequired;
    private Integer displayOrder;
    private Long groupId;
    private String groupName;
    private String appName;
    private Boolean active;
    private Integer version;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}
