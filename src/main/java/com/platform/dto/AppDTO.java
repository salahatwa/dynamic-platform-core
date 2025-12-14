package com.platform.dto;

import com.platform.entity.App;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppDTO {
    private Long id;
    private Long corporateId;
    private String name;
    private String description;
    private String appKey;
    private String iconUrl;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Statistics (optional, populated when needed)
    private Long translationCount;
    private Long templateCount;
    private Long lovCount;
    private Long configCount;
    private Long errorCodeCount;
    
    public static AppDTO fromEntity(App app) {
        AppDTO dto = new AppDTO();
        dto.setId(app.getId());
        dto.setCorporateId(app.getCorporate().getId());
        dto.setName(app.getName());
        dto.setDescription(app.getDescription());
        dto.setAppKey(app.getAppKey());
        dto.setIconUrl(app.getIconUrl());
        dto.setStatus(app.getStatus().name());
        if (app.getCreatedBy() != null) {
            dto.setCreatedBy(app.getCreatedBy().getId());
        }
        dto.setCreatedAt(app.getCreatedAt());
        dto.setUpdatedAt(app.getUpdatedAt());
        return dto;
    }
}
