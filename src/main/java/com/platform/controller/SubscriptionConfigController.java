package com.platform.controller;

import com.platform.config.SubscriptionProperties;
import com.platform.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/subscription-config")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SubscriptionConfigController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionProperties subscriptionProperties;

    /**
     * Get all subscription tier configurations
     */
    @GetMapping("/tiers")
    public ResponseEntity<Map<String, SubscriptionProperties.TierConfig>> getAllTierConfigurations() {
        log.info("Getting all subscription tier configurations");
        Map<String, SubscriptionProperties.TierConfig> configurations = subscriptionService.getAllTierConfigurations();
        return ResponseEntity.ok(configurations);
    }

    /**
     * Get specific tier configuration
     */
    @GetMapping("/tiers/{tierName}")
    public ResponseEntity<SubscriptionProperties.TierConfig> getTierConfiguration(@PathVariable String tierName) {
        log.info("Getting configuration for tier: {}", tierName);
        SubscriptionProperties.TierConfig config = subscriptionService.getTierConfiguration(tierName);
        return ResponseEntity.ok(config);
    }

    /**
     * Get current subscription properties (for debugging/admin purposes)
     */
    @GetMapping("/properties")
    public ResponseEntity<SubscriptionProperties> getSubscriptionProperties() {
        log.info("Getting subscription properties");
        return ResponseEntity.ok(subscriptionProperties);
    }
}