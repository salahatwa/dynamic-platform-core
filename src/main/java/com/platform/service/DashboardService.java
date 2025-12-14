package com.platform.service;

import com.platform.dto.DashboardStatsDto;
import com.platform.dto.RecentActivityDto;
import com.platform.entity.*;
import com.platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AppRepository appRepository;
    private final UserRepository userRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final TemplateRepository templateRepository;
    private final TranslationKeyRepository translationKeyRepository;
    private final TranslationRepository translationRepository;
    private final LovRepository lovRepository;
    private final AppConfigRepository appConfigRepository;
    private final AppConfigGroupRepository appConfigGroupRepository;
    private final ErrorCodeRepository errorCodeRepository;
    private final TranslationAppRepository translationAppRepository;

    /**
     * Get comprehensive dashboard statistics
     * @param appId Optional app ID for app-specific stats
     * @return Dashboard statistics
     */
    public DashboardStatsDto getDashboardStats(Long appId) {
        log.info("Getting dashboard stats for appId: {}", appId);
        
        DashboardStatsDto.DashboardStatsDtoBuilder builder = DashboardStatsDto.builder();
        
        if (appId != null) {
            // App-specific stats
            builder = buildAppSpecificStats(builder, appId);
        } else {
            // Global stats
            builder = buildGlobalStats(builder);
        }
        
        // Common activity stats
        builder = buildActivityStats(builder, appId);
        
        return builder.build();
    }

    /**
     * Build global statistics (when no app is selected)
     */
    private DashboardStatsDto.DashboardStatsDtoBuilder buildGlobalStats(DashboardStatsDto.DashboardStatsDtoBuilder builder) {
        return builder
                .totalApps(appRepository.count())
                .totalUsers(userRepository.count())
                .totalApiKeys(apiKeyRepository.count())
                // Initialize app-specific stats to zero
                .templates(DashboardStatsDto.TemplateStats.builder()
                        .total(0L).active(0L).change(0.0).build())
                .translations(DashboardStatsDto.TranslationStats.builder()
                        .totalKeys(0L).totalTranslations(0L).languages(0L).change(0.0).build())
                .lov(DashboardStatsDto.LovStats.builder()
                        .totalLists(0L).totalValues(0L).change(0.0).build())
                .appConfig(DashboardStatsDto.AppConfigStats.builder()
                        .totalConfigs(0L).totalGroups(0L).change(0.0).build())
                .errorCodes(DashboardStatsDto.ErrorCodeStats.builder()
                        .total(0L).active(0L).change(0.0).build());
    }

    /**
     * Build app-specific statistics
     */
    private DashboardStatsDto.DashboardStatsDtoBuilder buildAppSpecificStats(DashboardStatsDto.DashboardStatsDtoBuilder builder, Long appId) {
        // Template stats - now app-specific
        long totalTemplates = templateRepository.countByApp_Id(appId);
        long activeTemplates = templateRepository.countByApp_IdAndActiveTrue(appId);
        
        // Translation stats
        Long translationAppId = getTranslationAppId(appId);
        long totalKeys = translationAppId != null ? translationKeyRepository.countByApp_Id(translationAppId) : 0;
        long totalTranslations = translationAppId != null ? translationRepository.countByTranslationAppId(translationAppId) : 0;
        long languages = translationAppId != null ? translationRepository.countDistinctLanguagesByTranslationAppId(translationAppId) : 0;
        
        // LOV stats - now app-specific
        long totalLovLists = lovRepository.countByApp_Id(appId);
//        long totalLovValues = lovValueRepository.countByLovAppId(appId);
        
        // App Config stats - now app-specific
        long totalConfigs = appConfigRepository.countByApp_Id(appId);
        long totalGroups = appConfigGroupRepository.countByApp_Id(appId);
        
        // Error Code stats - now app-specific
        long totalErrorCodes = errorCodeRepository.countByApp_Id(appId);
        long activeErrorCodes = errorCodeRepository.countByApp_IdAndStatus(appId, ErrorCode.ErrorStatus.ACTIVE);
        
        // API Keys stats - now app-specific
        long totalApiKeys = apiKeyRepository.countByApp_Id(appId);
        long activeApiKeys = apiKeyRepository.countByApp_IdAndActive(appId, true);
        
        return builder
                .totalApps(0L)
                .totalUsers(0L)
                .totalApiKeys(totalApiKeys)
                .templates(DashboardStatsDto.TemplateStats.builder()
                        .total(totalTemplates)
                        .active(activeTemplates)
                        .change(calculateChange(totalTemplates, "templates"))
                        .build())
                .translations(DashboardStatsDto.TranslationStats.builder()
                        .totalKeys(totalKeys)
                        .totalTranslations(totalTranslations)
                        .languages(languages)
                        .change(calculateChange(totalKeys, "translations"))
                        .build())
                .lov(DashboardStatsDto.LovStats.builder()
                        .totalLists(totalLovLists)
//                        .totalValues(totalLovValues)
                        .change(calculateChange(totalLovLists, "lov"))
                        .build())
                .appConfig(DashboardStatsDto.AppConfigStats.builder()
                        .totalConfigs(totalConfigs)
                        .totalGroups(totalGroups)
                        .change(calculateChange(totalConfigs, "config"))
                        .build())
                .errorCodes(DashboardStatsDto.ErrorCodeStats.builder()
                        .total(totalErrorCodes)
                        .active(activeErrorCodes)
                        .change(calculateChange(totalErrorCodes, "errorCodes"))
                        .build());
    }

    /**
     * Build activity statistics
     */
    private DashboardStatsDto.DashboardStatsDtoBuilder buildActivityStats(DashboardStatsDto.DashboardStatsDtoBuilder builder, Long appId) {
        // For now, return mock data - in real implementation, these would come from audit logs
        return builder
                .pdfsGenerated(appId != null ? 1247L : 5847L)
                .pdfsChange(appId != null ? 23.8 : 32.1)
                .activeUsers(appId != null ? 45L : 89L)
                .usersChange(appId != null ? 18.5 : 12.3);
    }

    /**
     * Get translation app ID for the given app ID
     * Assumes TranslationApp name matches App name within the same corporate
     */
    private Long getTranslationAppId(Long appId) {
        if (appId == null) return null;
        
        return appRepository.findById(appId)
                .flatMap(app -> translationAppRepository.findByCorporateIdAndName(
                    app.getCorporate().getId(), 
                    app.getName()))
                .map(TranslationApp::getId)
                .orElse(null);
    }

    /**
     * Get app name from app ID
     */
    private String getAppName(Long appId) {
        if (appId == null) return null;
        return appRepository.findById(appId)
                .map(App::getName)
                .orElse(null);
    }

    /**
     * Get corporate ID from app ID
     */
    private Long getCorporateId(Long appId) {
        if (appId == null) return null;
        return appRepository.findById(appId)
                .map(app -> app.getCorporate().getId())
                .orElse(null);
    }

    /**
     * Calculate percentage change (mock implementation)
     * In real implementation, this would compare with previous period
     */
    private Double calculateChange(Long currentValue, String type) {
        // Mock calculation - in real implementation, compare with previous period
        if (currentValue == 0) return 0.0;
        
        // Return different mock values based on type
        switch (type) {
            case "templates": return 15.2;
            case "translations": return 8.7;
            case "lov": return 12.3;
            case "config": return 5.1;
            case "errorCodes": return -2.4;
            default: return 0.0;
        }
    }

    /**
     * Get recent activity
     * @param appId Optional app ID for app-specific activity
     * @param pageable Pagination parameters
     * @return Recent activity page
     */
    public Page<RecentActivityDto> getRecentActivity(Long appId, Pageable pageable) {
        log.info("Getting recent activity for appId: {}", appId);
        
        // For now, return mock data
        // In real implementation, this would query an audit log table
        List<RecentActivityDto> mockActivity = createMockActivity(appId);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), mockActivity.size());
        
        List<RecentActivityDto> pageContent = mockActivity.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, mockActivity.size());
    }

    /**
     * Get audit logs (future implementation)
     * @param appId Optional app ID for app-specific logs
     * @param pageable Pagination parameters
     * @return Audit logs page
     */
    public Page<RecentActivityDto> getAuditLogs(Long appId, Pageable pageable) {
        log.info("Getting audit logs for appId: {}", appId);
        
        // For now, return empty page
        // In real implementation, this would query a comprehensive audit log table
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    /**
     * Create mock activity data
     */
    private List<RecentActivityDto> createMockActivity(Long appId) {
        List<RecentActivityDto> activities = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        activities.add(RecentActivityDto.builder()
                .id(1L)
                .action("TEMPLATE_CREATED")
                .entityType("Template")
                .entityName("Invoice Template v2.1")
                .userName("John Doe")
                .userEmail("john.doe@example.com")
                .timestamp(now.minusMinutes(5))
                .details("Created new invoice template with enhanced styling")
                .ipAddress("192.168.1.100")
                .build());

        activities.add(RecentActivityDto.builder()
                .id(2L)
                .action("TRANSLATION_UPDATED")
                .entityType("Translation")
                .entityName("auth.login.title")
                .userName("Sarah Wilson")
                .userEmail("sarah.wilson@example.com")
                .timestamp(now.minusMinutes(15))
                .details("Updated Arabic translation")
                .ipAddress("192.168.1.101")
                .build());

        activities.add(RecentActivityDto.builder()
                .id(3L)
                .action("LOV_CREATED")
                .entityType("LOV")
                .entityName("COUNTRY_CODES")
                .userName("Mike Johnson")
                .userEmail("mike.johnson@example.com")
                .timestamp(now.minusMinutes(30))
                .details("Added new country codes list")
                .ipAddress("192.168.1.102")
                .build());

        activities.add(RecentActivityDto.builder()
                .id(4L)
                .action("CONFIG_UPDATED")
                .entityType("Config")
                .entityName("MAX_FILE_SIZE")
                .userName("Admin User")
                .userEmail("admin@example.com")
                .timestamp(now.minusMinutes(45))
                .details("Increased maximum file upload size")
                .ipAddress("192.168.1.1")
                .build());

        activities.add(RecentActivityDto.builder()
                .id(5L)
                .action("ERROR_CODE_CREATED")
                .entityType("ErrorCode")
                .entityName("AUTH_001")
                .userName("John Doe")
                .userEmail("john.doe@example.com")
                .timestamp(now.minusHours(1))
                .details("Added authentication error code")
                .ipAddress("192.168.1.100")
                .build());

        return activities;
    }
}