package com.platform.media.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleDriveConfig {
    private String serviceAccountJson;
    private String folderId;
    private String applicationName;
    private boolean sharePublicly;
    
    public static GoogleDriveConfig fromMap(java.util.Map<String, Object> configMap) {
        return GoogleDriveConfig.builder()
            .serviceAccountJson((String) configMap.get("serviceAccountJson"))
            .folderId((String) configMap.get("folderId"))
            .applicationName((String) configMap.getOrDefault("applicationName", "Dynamic Platform"))
            .sharePublicly(Boolean.parseBoolean(String.valueOf(configMap.getOrDefault("sharePublicly", false))))
            .build();
    }
}