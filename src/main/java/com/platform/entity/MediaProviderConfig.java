package com.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "media_provider_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MediaProviderConfig extends BaseEntity {

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "app_id")
    private Long appId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private MediaFile.MediaProviderType providerType;

    @Column(name = "provider_name")
    private String providerName;

    @Column(columnDefinition = "TEXT")
    private String credentials;

    @Column(columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_file_size_mb")
    @Builder.Default
    private Integer maxFileSizeMb = 100;

    @Column(name = "allowed_mime_types", columnDefinition = "TEXT")
    private String allowedMimeTypes;

    @Column(name = "public_base_url")
    private String publicBaseUrl;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Helper methods
    public boolean supportsPublicUrls() {
        return publicBaseUrl != null && !publicBaseUrl.trim().isEmpty();
    }

    public boolean isFileTypeAllowed(String mimeType) {
        if (allowedMimeTypes == null || allowedMimeTypes.trim().isEmpty()) {
            return true; // Allow all if not specified
        }
        
        String[] allowed = allowedMimeTypes.split(",");
        for (String type : allowed) {
            if (mimeType.matches(type.trim().replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }

    public boolean isFileSizeAllowed(long fileSizeBytes) {
        long maxSizeBytes = maxFileSizeMb * 1024L * 1024L;
        return fileSizeBytes <= maxSizeBytes;
    }
}