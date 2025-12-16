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
        FREE,
        PRO,
        TEAM,
        ENTERPRISE;
        
        // Note: The actual limits are now configured via SubscriptionProperties
        // This enum now only represents the tier names
        
        @Override
        public String toString() {
            return name();
        }
    }
    
    public enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        SUSPENDED
    }
    
    // Helper methods
    public boolean canCreateApp(int currentAppCount) {
        if (maxApps == -1) return true; // Unlimited
        return currentAppCount < maxApps;
    }
    
    public boolean canAddUser(int currentUserCount) {
        if (maxUsers == -1) return true; // Unlimited
        return currentUserCount < maxUsers;
    }
    
    public boolean canMakeApiRequest(long currentMonthRequests) {
        if (maxApiRequestsPerMonth == -1) return true; // Unlimited
        return currentMonthRequests < maxApiRequestsPerMonth;
    }
    
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && 
               (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
}
