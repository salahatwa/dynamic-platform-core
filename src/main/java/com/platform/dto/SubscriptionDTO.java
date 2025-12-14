package com.platform.dto;

import com.platform.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {
    private Long id;
    private Long corporateId;
    private String tier;
    private Integer maxApps;
    private Integer maxUsers;
    private Long maxApiRequestsPerMonth;
    private String features; // JSON string
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper flags
    private Boolean isUnlimitedApps;
    private Boolean isUnlimitedUsers;
    private Boolean isUnlimitedApiRequests;
    private Boolean isActive;
    
    public static SubscriptionDTO fromEntity(Subscription subscription) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(subscription.getId());
        dto.setCorporateId(subscription.getCorporate().getId());
        dto.setTier(subscription.getTier().name());
        dto.setMaxApps(subscription.getMaxApps());
        dto.setMaxUsers(subscription.getMaxUsers());
        dto.setMaxApiRequestsPerMonth(subscription.getMaxApiRequestsPerMonth());
        dto.setFeatures(subscription.getFeatures());
        dto.setStatus(subscription.getStatus().name());
        dto.setStartedAt(subscription.getStartedAt());
        dto.setExpiresAt(subscription.getExpiresAt());
        dto.setCreatedAt(subscription.getCreatedAt());
        dto.setUpdatedAt(subscription.getUpdatedAt());
        
        // Set helper flags
        dto.setIsUnlimitedApps(subscription.getMaxApps() == -1);
        dto.setIsUnlimitedUsers(subscription.getMaxUsers() == -1);
        dto.setIsUnlimitedApiRequests(subscription.getMaxApiRequestsPerMonth() == -1);
        dto.setIsActive(subscription.isActive());
        
        return dto;
    }
}
