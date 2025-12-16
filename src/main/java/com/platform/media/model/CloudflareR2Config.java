package com.platform.media.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudflareR2Config {
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;
    private String accountId;
    private String endpoint;
    private String region;
    private boolean publicAccess;
    private String customDomain;
    
    public static CloudflareR2Config fromMap(java.util.Map<String, Object> configMap) {
        return CloudflareR2Config.builder()
            .accessKeyId((String) configMap.get("accessKeyId"))
            .secretAccessKey((String) configMap.get("secretAccessKey"))
            .bucketName((String) configMap.get("bucketName"))
            .accountId((String) configMap.get("accountId"))
            .endpoint((String) configMap.get("endpoint"))
            .region((String) configMap.getOrDefault("region", "auto"))
            .publicAccess(Boolean.parseBoolean(String.valueOf(configMap.getOrDefault("publicAccess", false))))
            .customDomain((String) configMap.get("customDomain"))
            .build();
    }
    
    public String getEndpointUrl() {
        if (endpoint != null && !endpoint.isEmpty()) {
            return endpoint;
        }
        if (accountId != null && !accountId.isEmpty()) {
            // Use the standard Cloudflare R2 endpoint format
            return String.format("https://%s.r2.cloudflarestorage.com", accountId);
        }
        return "https://r2.cloudflarestorage.com";
    }
    
    /**
     * Get alternative endpoint URLs to try if the primary fails
     */
    public String[] getAlternativeEndpoints() {
        if (accountId != null && !accountId.isEmpty()) {
            return new String[] {
                String.format("https://%s.r2.cloudflarestorage.com", accountId),
                "https://r2.cloudflarestorage.com",
                String.format("https://s3.%s.r2.cloudflarestorage.com", accountId)
            };
        }
        return new String[] {
            "https://r2.cloudflarestorage.com"
        };
    }
    
    public String getRegion() {
        if (region == null || region.trim().isEmpty()) {
            return "auto";
        }
        return region;
    }
}