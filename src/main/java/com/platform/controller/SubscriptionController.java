package com.platform.controller;

import com.platform.dto.SubscriptionDTO;
import com.platform.dto.UsageLimitsDTO;
import com.platform.entity.Subscription;
import com.platform.security.UserPrincipal;
import com.platform.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    /**
     * Get corporate ID from authenticated user
     */
    private Long getCorporateId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long corporateId = userPrincipal.getCorporateId();
            
            if (corporateId == null) {
                throw new RuntimeException("User is not associated with any organization");
            }
            
            return corporateId;
        }
        
        throw new RuntimeException("Authentication required");
    }
    
    /**
     * Get current subscription
     */
    @GetMapping
    public ResponseEntity<SubscriptionDTO> getSubscription() {
        Long corporateId = getCorporateId();
        SubscriptionDTO subscription = subscriptionService.getSubscription(corporateId);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Get active subscription
     */
    @GetMapping("/active")
    public ResponseEntity<SubscriptionDTO> getActiveSubscription() {
        Long corporateId = getCorporateId();
        SubscriptionDTO subscription = subscriptionService.getActiveSubscription(corporateId);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Get usage limits and current usage
     */
    @GetMapping("/limits")
    public ResponseEntity<UsageLimitsDTO> getUsageLimits() {
        Long corporateId = getCorporateId();
        UsageLimitsDTO limits = subscriptionService.getUsageLimits(corporateId);
        return ResponseEntity.ok(limits);
    }
    
    /**
     * Check if can create app
     */
    @GetMapping("/can-create-app")
    public ResponseEntity<Boolean> canCreateApp() {
        Long corporateId = getCorporateId();
        boolean canCreate = subscriptionService.canCreateApp(corporateId);
        return ResponseEntity.ok(canCreate);
    }
    
    /**
     * Check if can add user
     */
    @GetMapping("/can-add-user")
    public ResponseEntity<Boolean> canAddUser() {
        Long corporateId = getCorporateId();
        boolean canAdd = subscriptionService.canAddUser(corporateId);
        return ResponseEntity.ok(canAdd);
    }
    
    /**
     * Upgrade subscription
     */
    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionDTO> upgradeSubscription(@RequestParam String tier) {
        Long corporateId = getCorporateId();
        
        Subscription.SubscriptionTier newTier;
        try {
            newTier = Subscription.SubscriptionTier.valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid subscription tier: " + tier);
        }
        
        SubscriptionDTO subscription = subscriptionService.upgradeSubscription(corporateId, newTier);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Get current month API usage
     */
    @GetMapping("/api-usage")
    public ResponseEntity<Long> getCurrentMonthApiUsage() {
        Long corporateId = getCorporateId();
        long usage = subscriptionService.getCurrentMonthApiRequests(corporateId);
        return ResponseEntity.ok(usage);
    }
}
