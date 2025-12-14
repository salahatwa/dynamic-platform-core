package com.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    
    // Global Stats
    private Long totalApps;
    private Long totalUsers;
    private Long totalApiKeys;
    
    // App-Specific Stats (when app is selected)
    private TemplateStats templates;
    private TranslationStats translations;
    private LovStats lov;
    private AppConfigStats appConfig;
    private ErrorCodeStats errorCodes;
    
    // Activity Stats
    private Long pdfsGenerated;
    private Double pdfsChange;
    private Long activeUsers;
    private Double usersChange;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStats {
        private Long total;
        private Long active;
        private Double change;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranslationStats {
        private Long totalKeys;
        private Long totalTranslations;
        private Long languages;
        private Double change;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LovStats {
        private Long totalLists;
        private Long totalValues;
        private Double change;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppConfigStats {
        private Long totalConfigs;
        private Long totalGroups;
        private Double change;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorCodeStats {
        private Long total;
        private Long active;
        private Double change;
    }
}