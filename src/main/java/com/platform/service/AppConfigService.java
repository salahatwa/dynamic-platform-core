package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.dto.AppConfigDTO;
import com.platform.dto.AppConfigGroupRequest;
import com.platform.dto.AppConfigRequest;
import com.platform.entity.AppConfig;
import com.platform.entity.AppConfigAudit;
import com.platform.entity.AppConfigGroup;
import com.platform.entity.AppConfigVersion;
import com.platform.repository.AppConfigAuditRepository;
import com.platform.repository.AppConfigGroupRepository;
import com.platform.repository.AppConfigRepository;
import com.platform.repository.AppConfigVersionRepository;
import com.platform.repository.AppRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppConfigService {
    
    private final AppConfigRepository configRepository;
    private final AppConfigGroupRepository groupRepository;
    private final AppConfigVersionRepository versionRepository;
    private final AppConfigAuditRepository auditRepository;
    private final AppRepository appRepository;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;
    
    // ==================== Configuration CRUD ====================
    
    @Cacheable(value = "appConfigs", key = "#corporateId + '_' + #appName + '_' + #appId")
    public List<AppConfig> getAllConfigs(Long corporateId, String appName, Long appId, Long groupId, Boolean active) {
        // Handle appId filtering first
        if (appId != null) {
            if (groupId != null && active != null) {
                return configRepository.findByApp_IdAndActive(appId, active)
                    .stream()
                    .filter(c -> c.getCorporateId().equals(corporateId))
                    .filter(c -> c.getGroupId() != null && c.getGroupId().equals(groupId))
                    .collect(Collectors.toList());
            } else if (active != null) {
                return configRepository.findByApp_IdAndActive(appId, active)
                    .stream()
                    .filter(c -> c.getCorporateId().equals(corporateId))
                    .collect(Collectors.toList());
            } else {
                return configRepository.findByApp_Id(appId)
                    .stream()
                    .filter(c -> c.getCorporateId().equals(corporateId))
                    .collect(Collectors.toList());
            }
        }
        
        // Handle appName filtering (existing logic)
        if (appName != null && groupId != null && active != null) {
            return configRepository.findByCorporateIdAndAppNameAndActive(corporateId, appName, active)
                .stream()
                .filter(c -> c.getGroupId() != null && c.getGroupId().equals(groupId))
                .collect(Collectors.toList());
        } else if (appName != null && active != null) {
            return configRepository.findByCorporateIdAndAppNameAndActive(corporateId, appName, active);
        } else if (appName != null) {
            return configRepository.findByCorporateIdAndAppName(corporateId, appName);
        } else if (groupId != null) {
            return configRepository.findByCorporateIdAndGroupId(corporateId, groupId);
        } else {
            return configRepository.findByCorporateId(corporateId);
        }
    }
    
    public List<AppConfigDTO> getAllConfigsWithGroupNames(Long corporateId, String appName, Long appId, Long groupId, Boolean active, int page, int size) {
        List<AppConfig> configs = getAllConfigs(corporateId, appName, appId, groupId, active);
        Map<Long, String> groupNames = new HashMap<>();
        
        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, configs.size());
        
        if (startIndex >= configs.size()) {
            return List.of(); // Return empty list if page is beyond available data
        }
        
        List<AppConfig> paginatedConfigs = configs.subList(startIndex, endIndex);
        
        return paginatedConfigs.stream().map(config -> {
            AppConfigDTO dto = mapToDTO(config);
            
            // Get group name if groupId exists
            if (config.getGroupId() != null) {
                String groupName = groupNames.computeIfAbsent(config.getGroupId(), id -> {
                    return groupRepository.findById(id)
                        .map(AppConfigGroup::getGroupName)
                        .orElse(null);
                });
                dto.setGroupName(groupName);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    public AppConfig getConfigById(Long id, Long corporateId) {
        AppConfig config = configRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration not found"));
        
        if (!config.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        logAudit(config.getId(), AppConfigAudit.Action.VIEW, null, null);
        return config;
    }
    
    public AppConfig getConfigByKey(String configKey, String appName, Long corporateId) {
        AppConfig config = configRepository.findByConfigKeyAndAppNameAndCorporateId(configKey, appName, corporateId)
            .orElseThrow(() -> new RuntimeException("Configuration not found"));
        
        logAudit(config.getId(), AppConfigAudit.Action.VIEW, null, null);
        return config;
    }
    
    @Cacheable(value = "publicConfigs", key = "#appName + '_' + #corporateId")
    public List<AppConfig> getPublicConfigs(String appName, Long corporateId) {
        return configRepository.findByCorporateIdAndAppNameAndIsPublic(corporateId, appName, true);
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public AppConfig createConfig(AppConfigRequest request, Long corporateId) {
        // Check if config key already exists
        if (configRepository.existsByConfigKeyAndAppNameAndCorporateId(
                request.getConfigKey(), request.getAppName(), corporateId)) {
            throw new RuntimeException("Configuration key already exists for this app");
        }
        
        AppConfig config = new AppConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigName(request.getConfigName());
        config.setDescription(request.getDescription());
        config.setConfigType(request.getConfigType());
        config.setConfigValue(request.getConfigValue());
        config.setDefaultValue(request.getDefaultValue());
        config.setEnumValues(request.getEnumValues());
        config.setValidationRules(request.getValidationRules());
        config.setIsPublic(request.getIsPublic());
        config.setIsRequired(request.getIsRequired());
        config.setDisplayOrder(request.getDisplayOrder());
        config.setGroupId(request.getGroupId());
        
        // Set App entity instead of appName
        if (request.getAppName() != null && !request.getAppName().isBlank()) {
            appRepository.findByCorporateIdAndName(corporateId, request.getAppName())
                .ifPresent(config::setApp);
        }
        
        config.setActive(request.getActive());
        config.setCorporateId(corporateId);
        config.setVersion(1);
        config.setCreatedBy(getCurrentUsername());
        config.setUpdatedBy(getCurrentUsername());
        
        // Validate config value
        validateConfigValue(config);
        
        AppConfig savedConfig = configRepository.save(config);
        
        // Create version
        createVersion(savedConfig, AppConfigVersion.ChangeType.CREATE, "Initial creation");
        
        // Log audit
        logAudit(savedConfig.getId(), AppConfigAudit.Action.CREATE, null, savedConfig.getConfigValue());
        
        return savedConfig;
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public AppConfig updateConfig(Long id, AppConfigRequest request, Long corporateId) {
        AppConfig config = configRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration not found"));
        
        if (!config.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        // Track old value for audit
        String oldValue = config.getConfigValue();
        
        // Update fields
        config.setConfigName(request.getConfigName());
        config.setDescription(request.getDescription());
        config.setConfigType(request.getConfigType());
        config.setConfigValue(request.getConfigValue());
        config.setDefaultValue(request.getDefaultValue());
        config.setEnumValues(request.getEnumValues());
        config.setValidationRules(request.getValidationRules());
        config.setIsPublic(request.getIsPublic());
        config.setIsRequired(request.getIsRequired());
        config.setDisplayOrder(request.getDisplayOrder());
        config.setGroupId(request.getGroupId());
        config.setActive(request.getActive());
        config.setUpdatedBy(getCurrentUsername());
        config.setVersion(config.getVersion() + 1);
        
        // Validate config value
        validateConfigValue(config);
        
        AppConfig updatedConfig = configRepository.save(config);
        
        // Create version
        createVersion(updatedConfig, AppConfigVersion.ChangeType.UPDATE, "Configuration updated");
        
        // Log audit
        logAudit(updatedConfig.getId(), AppConfigAudit.Action.UPDATE, oldValue, updatedConfig.getConfigValue());
        
        return updatedConfig;
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public void deleteConfig(Long id, Long corporateId) {
        AppConfig config = configRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration not found"));
        
        if (!config.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        // Create final version before deletion
        createVersion(config, AppConfigVersion.ChangeType.DELETE, "Configuration deleted");
        
        // Log audit
        logAudit(config.getId(), AppConfigAudit.Action.DELETE, config.getConfigValue(), null);
        
        configRepository.delete(config);
    }
    
    // ==================== Configuration Groups ====================
    
    public List<AppConfigGroup> getAllGroups(Long corporateId, String appName, Long appId, Boolean active) {
        // Handle appId filtering first
        if (appId != null) {
            if (active != null) {
                return groupRepository.findByApp_IdAndActive(appId, active)
                    .stream()
                    .filter(g -> g.getCorporateId().equals(corporateId))
                    .collect(Collectors.toList());
            } else {
                return groupRepository.findByApp_Id(appId)
                    .stream()
                    .filter(g -> g.getCorporateId().equals(corporateId))
                    .collect(Collectors.toList());
            }
        }
        
        // Handle appName filtering (existing logic)
        if (appName != null && active != null) {
            return groupRepository.findByCorporateIdAndAppNameAndActive(corporateId, appName, active);
        } else if (appName != null) {
            return groupRepository.findByCorporateIdAndAppName(corporateId, appName);
        } else if (active != null) {
            return groupRepository.findByCorporateIdAndActive(corporateId, active);
        } else {
            return groupRepository.findByCorporateId(corporateId);
        }
    }
    
    public AppConfigGroup getGroupById(Long id, Long corporateId) {
        AppConfigGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration group not found"));
        
        if (!group.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        return group;
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public AppConfigGroup createGroup(AppConfigGroupRequest request, Long corporateId) {
        // Check if group key already exists
        if (groupRepository.existsByGroupKeyAndAppNameAndCorporateId(
                request.getGroupKey(), request.getAppName(), corporateId)) {
            throw new RuntimeException("Group key already exists for this app");
        }
        
        AppConfigGroup group = new AppConfigGroup();
        group.setGroupKey(request.getGroupKey());
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        
        // Set App entity instead of appName
        if (request.getAppName() != null && !request.getAppName().isBlank()) {
            appRepository.findByCorporateIdAndName(corporateId, request.getAppName())
                .ifPresent(group::setApp);
        }
        
        group.setDisplayOrder(request.getDisplayOrder());
        group.setActive(request.getActive());
        group.setCorporateId(corporateId);
        group.setCreatedBy(getCurrentUsername());
        group.setUpdatedBy(getCurrentUsername());
        
        return groupRepository.save(group);
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public AppConfigGroup updateGroup(Long id, AppConfigGroupRequest request, Long corporateId) {
        AppConfigGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration group not found"));
        
        if (!group.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        group.setDisplayOrder(request.getDisplayOrder());
        group.setActive(request.getActive());
        group.setUpdatedBy(getCurrentUsername());
        
        return groupRepository.save(group);
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public void deleteGroup(Long id, Long corporateId) {
        AppConfigGroup group = groupRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Configuration group not found"));
        
        if (!group.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        // Check if group has configs
        List<AppConfig> configs = configRepository.findByCorporateIdAndGroupId(corporateId, id);
        if (!configs.isEmpty()) {
            throw new RuntimeException("Cannot delete group with existing configurations. Remove or reassign configurations first.");
        }
        
        groupRepository.delete(group);
    }
    
    // ==================== Versioning ====================
    
    public List<AppConfigVersion> getConfigVersions(Long configId, Long corporateId) {
        AppConfig config = getConfigById(configId, corporateId);
        return versionRepository.findByConfigIdOrderByVersionDesc(config.getId());
    }
    
    @Transactional
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public AppConfig restoreVersion(Long configId, Long versionId, Long corporateId) {
        AppConfig config = getConfigById(configId, corporateId);
        
        AppConfigVersion version = versionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        
        if (!version.getConfigId().equals(configId)) {
            throw new RuntimeException("Version does not belong to this configuration");
        }
        
        String oldValue = config.getConfigValue();
        config.setConfigValue(version.getConfigValue());
        config.setVersion(config.getVersion() + 1);
        config.setUpdatedBy(getCurrentUsername());
        
        AppConfig restoredConfig = configRepository.save(config);
        
        // Create new version for restore action
        createVersion(restoredConfig, AppConfigVersion.ChangeType.UPDATE, 
            "Restored from version " + version.getVersion());
        
        // Log audit
        logAudit(restoredConfig.getId(), AppConfigAudit.Action.RESTORE, oldValue, restoredConfig.getConfigValue());
        
        return restoredConfig;
    }
    
    // ==================== Audit ====================
    
    public List<AppConfigAudit> getConfigAudit(Long configId, Long corporateId) {
        AppConfig config = getConfigById(configId, corporateId);
        return auditRepository.findByConfigIdOrderByTimestampDesc(config.getId());
    }
    
    // ==================== Utility Methods ====================
    
    public List<String> getAppNames(Long corporateId) {
        return configRepository.findDistinctAppNamesByCorporateId(corporateId);
    }
    
    @CacheEvict(value = {"appConfigs", "publicConfigs"}, allEntries = true)
    public void invalidateCache() {
        // Cache eviction handled by annotation
    }
    
    // Helper method to get config value as string
    public String getConfigValue(String configKey, String appName, Long corporateId) {
        return getConfigValue(configKey, appName, corporateId, null);
    }
    
    public String getConfigValue(String configKey, String appName, Long corporateId, String defaultValue) {
        try {
            AppConfig config = getConfigByKey(configKey, appName, corporateId);
            return config.getConfigValue() != null ? config.getConfigValue() : 
                   (config.getDefaultValue() != null ? config.getDefaultValue() : defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    // Helper method to get config value as boolean
    public Boolean getConfigValueAsBoolean(String configKey, String appName, Long corporateId) {
        String value = getConfigValue(configKey, appName, corporateId);
        return value != null ? Boolean.parseBoolean(value) : null;
    }
    
    // Helper method to get config value as integer
    public Integer getConfigValueAsInteger(String configKey, String appName, Long corporateId) {
        String value = getConfigValue(configKey, appName, corporateId);
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private void createVersion(AppConfig config, AppConfigVersion.ChangeType changeType, String description) {
        AppConfigVersion version = new AppConfigVersion();
        version.setConfigId(config.getId());
        version.setVersion(config.getVersion());
        version.setConfigValue(config.getConfigValue());
        version.setChangeType(changeType);
        version.setChangeDescription(description);
        version.setChangedBy(getCurrentUsername());
        
        versionRepository.save(version);
    }
    
    private void logAudit(Long configId, AppConfigAudit.Action action, String oldValue, String newValue) {
        AppConfigAudit audit = new AppConfigAudit();
        audit.setConfigId(configId);
        audit.setAction(action);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setUserEmail(getCurrentUsername());
        audit.setIpAddress(getClientIpAddress());
        audit.setUserAgent(request.getHeader("User-Agent"));
        
        auditRepository.save(audit);
    }
    
    private void validateConfigValue(AppConfig config) {
        if (config.getIsRequired() && (config.getConfigValue() == null || config.getConfigValue().trim().isEmpty())) {
            throw new RuntimeException("Configuration value is required");
        }
        
        // Type-specific validation
        switch (config.getConfigType()) {
            case NUMBER:
                if (config.getConfigValue() != null) {
                    try {
                        Double.parseDouble(config.getConfigValue());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid number format");
                    }
                }
                break;
            case BOOLEAN:
                if (config.getConfigValue() != null) {
                    String value = config.getConfigValue().toLowerCase();
                    if (!value.equals("true") && !value.equals("false")) {
                        throw new RuntimeException("Boolean value must be 'true' or 'false'");
                    }
                }
                break;
            case JSON:
                if (config.getConfigValue() != null) {
                    try {
                        objectMapper.readTree(config.getConfigValue());
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid JSON format");
                    }
                }
                break;
            case ENUM:
                if (config.getConfigValue() != null && config.getEnumValues() != null) {
                    try {
                        List<String> allowedValues = objectMapper.readValue(
                            config.getEnumValues(), 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                        );
                        if (!allowedValues.contains(config.getConfigValue())) {
                            throw new RuntimeException("Value must be one of: " + String.join(", ", allowedValues));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid enum configuration");
                    }
                }
                break;
        }
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
    
    private String getClientIpAddress() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private AppConfigDTO mapToDTO(AppConfig config) {
        AppConfigDTO dto = new AppConfigDTO();
        dto.setId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigName(config.getConfigName());
        dto.setDescription(config.getDescription());
        dto.setConfigType(config.getConfigType());
        dto.setConfigValue(config.getConfigValue());
        dto.setDefaultValue(config.getDefaultValue());
        dto.setEnumValues(config.getEnumValues());
        dto.setValidationRules(config.getValidationRules());
        dto.setIsPublic(config.getIsPublic());
        dto.setIsRequired(config.getIsRequired());
        dto.setDisplayOrder(config.getDisplayOrder());
        dto.setGroupId(config.getGroupId());
        dto.setAppName(config.getApp() != null ? config.getApp().getName() : null);
        dto.setActive(config.getActive());
        dto.setVersion(config.getVersion());
        dto.setCreatedBy(config.getCreatedBy());
        dto.setCreatedAt(config.getCreatedAt().toString());
        dto.setUpdatedBy(config.getUpdatedBy());
        dto.setUpdatedAt(config.getUpdatedAt().toString());
        return dto;
    }
}
