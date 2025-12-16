package com.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Component
@ConfigurationProperties(prefix = "subscription")
@Data
public class SubscriptionProperties {

    private Map<String, TierConfig> tiers = new HashMap<>();

    @Data
    public static class TierConfig {
        private int maxApps = 1;
        private int maxUsers = 2;
        private long maxApiRequests = 1000L;
        private String description = "";
        private boolean enabled = true;

        // Helper methods
        public boolean isUnlimitedApps() { 
            return maxApps == -1; 
        }
        
        public boolean isUnlimitedUsers() { 
            return maxUsers == -1; 
        }
        
        public boolean isUnlimitedApiRequests() { 
            return maxApiRequests == -1; 
        }
    }

    // Helper methods to get tier configurations
    public TierConfig getTierConfig(String tierName) {
        return tiers.getOrDefault(tierName.toUpperCase(), getDefaultFreeConfig());
    }

    public TierConfig getFreeConfig() {
        return tiers.getOrDefault("FREE", getDefaultFreeConfig());
    }

    public TierConfig getProConfig() {
        return tiers.getOrDefault("PRO", getDefaultProConfig());
    }

    public TierConfig getTeamConfig() {
        return tiers.getOrDefault("TEAM", getDefaultTeamConfig());
    }

    public TierConfig getEnterpriseConfig() {
        return tiers.getOrDefault("ENTERPRISE", getDefaultEnterpriseConfig());
    }

    // Default configurations (fallback if not configured in properties)
    private TierConfig getDefaultFreeConfig() {
        TierConfig config = new TierConfig();
        config.setMaxApps(2); // Updated default to 2 as requested
        config.setMaxUsers(2);
        config.setMaxApiRequests(1000L);
        config.setDescription("Free tier with basic features");
        config.setEnabled(true);
        return config;
    }

    private TierConfig getDefaultProConfig() {
        TierConfig config = new TierConfig();
        config.setMaxApps(10);
        config.setMaxUsers(-1); // Unlimited
        config.setMaxApiRequests(50000L);
        config.setDescription("Pro tier with advanced features");
        config.setEnabled(true);
        return config;
    }

    private TierConfig getDefaultTeamConfig() {
        TierConfig config = new TierConfig();
        config.setMaxApps(-1); // Unlimited
        config.setMaxUsers(50);
        config.setMaxApiRequests(500000L);
        config.setDescription("Team tier for organizations");
        config.setEnabled(true);
        return config;
    }

    private TierConfig getDefaultEnterpriseConfig() {
        TierConfig config = new TierConfig();
        config.setMaxApps(-1); // Unlimited
        config.setMaxUsers(-1); // Unlimited
        config.setMaxApiRequests(-1L); // Unlimited
        config.setDescription("Enterprise tier with unlimited features");
        config.setEnabled(true);
        return config;
    }
}