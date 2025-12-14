package com.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_id", nullable = false, unique = true)
    private Corporate corporate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionTier tier;
    
    @Column(name = "max_apps", nullable = false)
    private Integer maxApps;
    
    @Column(name = "max_users", nullable = false)
    private Integer maxUsers; // -1 for unlimited
    
    @Column(name = "max_api_requests_per_month", nullable = false)
    private Long maxApiRequestsPerMonth; // -1 for unlimited
    
    @Column(name = "features", insertable = false, updatable = false)
    private String features; // Store as JSON string (not managed by JPA for now)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum SubscriptionTier {
        FREE(1, 2, 1000L),
        PRO(10, -1, 50000L),
        TEAM(-1, 50, 500000L),
        ENTERPRISE(-1, -1, -1L);
        
        private final int maxApps;
        private final int maxUsers;
        private final long maxApiRequests;
        
        SubscriptionTier(int maxApps, int maxUsers, long maxApiRequests) {
            this.maxApps = maxApps;
            this.maxUsers = maxUsers;
            this.maxApiRequests = maxApiRequests;
        }
        
        public int getMaxApps() { return maxApps; }
        public int getMaxUsers() { return maxUsers; }
        public long getMaxApiRequests() { return maxApiRequests; }
        
        public boolean isUnlimitedApps() { return maxApps == -1; }
        public boolean isUnlimitedUsers() { return maxUsers == -1; }
        public boolean isUnlimitedApiRequests() { return maxApiRequests == -1; }
    }
    
    public enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        SUSPENDED
    }
    
    // Helper methods
    public boolean canCreateApp(int currentAppCount) {
        if (tier.isUnlimitedApps()) return true;
        return currentAppCount < maxApps;
    }
    
    public boolean canAddUser(int currentUserCount) {
        if (tier.isUnlimitedUsers()) return true;
        return currentUserCount < maxUsers;
    }
    
    public boolean canMakeApiRequest(long currentMonthRequests) {
        if (tier.isUnlimitedApiRequests()) return true;
        return currentMonthRequests < maxApiRequestsPerMonth;
    }
    
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && 
               (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
}
