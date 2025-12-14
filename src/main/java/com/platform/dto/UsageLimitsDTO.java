package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageLimitsDTO {
    private SubscriptionDTO subscription;
    private Long currentApps;
    private Long currentUsers;
    private Long currentApiRequests;
    private Boolean canCreateApp;
    private Boolean canAddUser;
    private Boolean canMakeApiRequest;
    private Map<String, Double> percentageUsed;
    
    public UsageLimitsDTO(SubscriptionDTO subscription, Long currentApps, Long currentUsers, Long currentApiRequests) {
        this.subscription = subscription;
        this.currentApps = currentApps;
        this.currentUsers = currentUsers;
        this.currentApiRequests = currentApiRequests;
        
        // Calculate permissions
        this.canCreateApp = subscription.getIsUnlimitedApps() || currentApps < subscription.getMaxApps();
        this.canAddUser = subscription.getIsUnlimitedUsers() || currentUsers < subscription.getMaxUsers();
        this.canMakeApiRequest = subscription.getIsUnlimitedApiRequests() || currentApiRequests < subscription.getMaxApiRequestsPerMonth();
        
        // Calculate percentage used
        this.percentageUsed = new HashMap<>();
        
        if (subscription.getIsUnlimitedApps()) {
            this.percentageUsed.put("apps", 0.0);
        } else {
            this.percentageUsed.put("apps", (currentApps.doubleValue() / subscription.getMaxApps()) * 100);
        }
        
        if (subscription.getIsUnlimitedUsers()) {
            this.percentageUsed.put("users", 0.0);
        } else {
            this.percentageUsed.put("users", (currentUsers.doubleValue() / subscription.getMaxUsers()) * 100);
        }
        
        if (subscription.getIsUnlimitedApiRequests()) {
            this.percentageUsed.put("apiRequests", 0.0);
        } else {
            this.percentageUsed.put("apiRequests", (currentApiRequests.doubleValue() / subscription.getMaxApiRequestsPerMonth()) * 100);
        }
    }
}
