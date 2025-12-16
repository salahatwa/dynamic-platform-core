package com.platform.service;

import com.platform.dto.ErrorCodeCategoryRequest;
import com.platform.dto.ErrorCodeDTO;
import com.platform.dto.ErrorCodeRequest;
import com.platform.dto.ErrorCodeSettingsRequest;
import com.platform.dto.ErrorCodeGenerationResponse;
import com.platform.entity.*;
import com.platform.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorCodeService {
    
    private final ErrorCodeRepository errorCodeRepository;
    private final ErrorCodeCategoryRepository categoryRepository;
    private final ErrorCodeTranslationRepository translationRepository;
    private final ErrorCodeVersionRepository versionRepository;
    private final ErrorCodeAuditRepository auditRepository;
    private final AppRepository appRepository;
    private final ErrorCodeSettingsRepository settingsRepository;
    private final HttpServletRequest request;

    // ==================== ERROR CODE CRUD ====================

    @Transactional
    @CacheEvict(value = "errorCodes", allEntries = true)
    public ErrorCodeDTO createErrorCode(ErrorCodeRequest req) {
        String username = getCurrentUsername();
        Long corporateId = getCurrentCorporateId();

        // Check for duplicate
        if (errorCodeRepository.existsByErrorCodeAndCorporateId(req.getErrorCode(), corporateId)) {
            throw new RuntimeException("Error code already exists: " + req.getErrorCode());
        }

        ErrorCode errorCode = new ErrorCode();
        errorCode.setErrorCode(req.getErrorCode());
        
        // Set App entity instead of appName
        if (req.getAppName() != null && !req.getAppName().isBlank()) {
            appRepository.findByCorporateIdAndName(corporateId, req.getAppName())
                .ifPresent(errorCode::setApp);
        }
        
        errorCode.setModuleName(req.getModuleName());
        errorCode.setSeverity(req.getSeverity());
        errorCode.setStatus(req.getStatus() != null ? req.getStatus() : ErrorCode.ErrorStatus.ACTIVE);
        errorCode.setHttpStatusCode(req.getHttpStatusCode());
        errorCode.setIsPublic(req.getIsPublic() != null ? req.getIsPublic() : true);
        errorCode.setIsRetryable(req.getIsRetryable() != null ? req.getIsRetryable() : false);
        errorCode.setDefaultMessage(req.getDefaultMessage());
        errorCode.setTechnicalDetails(req.getTechnicalDetails());
        errorCode.setResolutionSteps(req.getResolutionSteps());
        errorCode.setDocumentationUrl(req.getDocumentationUrl());
        errorCode.setCorporateId(corporateId);
        errorCode.setCreatedBy(username);
        errorCode.setUpdatedBy(username);

        if (req.getCategoryId() != null) {
            ErrorCodeCategory category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
            errorCode.setCategory(category);
        }

        errorCode = errorCodeRepository.save(errorCode);

        // Save translations
        if (req.getTranslations() != null && !req.getTranslations().isEmpty()) {
            for (Map.Entry<String, ErrorCodeRequest.TranslationData> entry : req.getTranslations().entrySet()) {
                ErrorCodeTranslation translation = new ErrorCodeTranslation();
                translation.setErrorCode(errorCode);
                translation.setLanguageCode(entry.getKey());
                translation.setMessage(entry.getValue().getMessage());
                translation.setTechnicalDetails(entry.getValue().getTechnicalDetails());
                translation.setResolutionSteps(entry.getValue().getResolutionSteps());
                translationRepository.save(translation);
            }
        }

        // Create version
        createVersion(errorCode, 1, "Initial creation", username);

        // Audit log
        logAudit(errorCode.getId(), "CREATE", null, null, errorCode.getErrorCode(), username);

        return toDTO(errorCode);
    }

    @Transactional
    @CacheEvict(value = "errorCodes", allEntries = true)
    public ErrorCodeDTO updateErrorCode(Long id, ErrorCodeRequest req) {
        String username = getCurrentUsername();
        Long corporateId = getCurrentCorporateId();

        ErrorCode errorCode = errorCodeRepository.findByIdAndCorporateId(id, corporateId)
            .orElseThrow(() -> new RuntimeException("Error code not found"));

        // Track changes for audit
        Map<String, String[]> changes = new HashMap<>();
        
        if (!errorCode.getErrorCode().equals(req.getErrorCode())) {
            changes.put("errorCode", new String[]{errorCode.getErrorCode(), req.getErrorCode()});
            errorCode.setErrorCode(req.getErrorCode());
        }
        
        if (req.getCategoryId() != null) {
            Long currentCategoryId = errorCode.getCategory() != null ? errorCode.getCategory().getId() : null;
            if (!Objects.equals(currentCategoryId, req.getCategoryId())) {
                ErrorCodeCategory category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
                errorCode.setCategory(category);
            }
        }

        // Update App entity instead of appName
        if (req.getAppName() != null && !req.getAppName().isBlank()) {
            appRepository.findByCorporateIdAndName(corporateId, req.getAppName())
                .ifPresent(errorCode::setApp);
        }
        
        errorCode.setModuleName(req.getModuleName());
        errorCode.setSeverity(req.getSeverity());
        errorCode.setStatus(req.getStatus());
        errorCode.setHttpStatusCode(req.getHttpStatusCode());
        errorCode.setIsPublic(req.getIsPublic());
        errorCode.setIsRetryable(req.getIsRetryable());
        errorCode.setDefaultMessage(req.getDefaultMessage());
        errorCode.setTechnicalDetails(req.getTechnicalDetails());
        errorCode.setResolutionSteps(req.getResolutionSteps());
        errorCode.setDocumentationUrl(req.getDocumentationUrl());
        errorCode.setUpdatedBy(username);

        errorCode = errorCodeRepository.save(errorCode);

        // Update translations
        if (req.getTranslations() != null) {
            translationRepository.deleteByErrorCodeId(errorCode.getId());
            for (Map.Entry<String, ErrorCodeRequest.TranslationData> entry : req.getTranslations().entrySet()) {
                ErrorCodeTranslation translation = new ErrorCodeTranslation();
                translation.setErrorCode(errorCode);
                translation.setLanguageCode(entry.getKey());
                translation.setMessage(entry.getValue().getMessage());
                translation.setTechnicalDetails(entry.getValue().getTechnicalDetails());
                translation.setResolutionSteps(entry.getValue().getResolutionSteps());
                translationRepository.save(translation);
            }
        }

        // Create new version
        int latestVersion = versionRepository.findMaxVersionByErrorCodeId(errorCode.getId()).orElse(0);
        createVersion(errorCode, latestVersion + 1, "Updated", username);

        // Audit changes
        for (Map.Entry<String, String[]> change : changes.entrySet()) {
            logAudit(errorCode.getId(), "UPDATE", change.getKey(), change.getValue()[0], change.getValue()[1], username);
        }

        return toDTO(errorCode);
    }

    @Transactional
    @CacheEvict(value = "errorCodes", allEntries = true)
    public void deleteErrorCode(Long id) {
        String username = getCurrentUsername();
        Long corporateId = getCurrentCorporateId();

        ErrorCode errorCode = errorCodeRepository.findByIdAndCorporateId(id, corporateId)
            .orElseThrow(() -> new RuntimeException("Error code not found"));

        logAudit(errorCode.getId(), "DELETE", null, errorCode.getErrorCode(), null, username);
        errorCodeRepository.delete(errorCode);
    }

    @Cacheable(value = "errorCodes", key = "#id + '_' + #corporateId")
    public ErrorCodeDTO getErrorCode(Long id) {
        Long corporateId = getCurrentCorporateId();
        ErrorCode errorCode = errorCodeRepository.findByIdAndCorporateId(id, corporateId)
            .orElseThrow(() -> new RuntimeException("Error code not found"));
        return toDTO(errorCode);
    }

    public Page<ErrorCodeDTO> getAllErrorCodes(String appName, Long categoryId, 
                                               ErrorCode.ErrorSeverity severity,
                                               ErrorCode.ErrorStatus status,
                                               String search, Pageable pageable) {
        Long corporateId = getCurrentCorporateId();
        String severityStr = severity != null ? severity.name() : null;
        String statusStr = status != null ? status.name() : null;
        Page<ErrorCode> page = errorCodeRepository.findAllWithFilters(
            corporateId, appName, categoryId, severityStr, statusStr, search, pageable);
        return page.map(this::toDTO);
    }

    public List<String> getDistinctApps() {
        Long corporateId = getCurrentCorporateId();
        return errorCodeRepository.findDistinctAppNamesByCorporateId(corporateId);
    }

    public List<String> getDistinctModules(String appName) {
        Long corporateId = getCurrentCorporateId();
        if (appName != null && !appName.isBlank()) {
            return errorCodeRepository.findDistinctModulesByAppName(appName, corporateId);
        }
        return new ArrayList<>();
    }

    // ==================== CATEGORY CRUD ====================

    @Transactional
    public ErrorCodeCategory createCategory(ErrorCodeCategoryRequest req) {
        String username = getCurrentUsername();
        Long corporateId = getCurrentCorporateId();

        ErrorCodeCategory category = new ErrorCodeCategory();
        category.setCategoryCode(req.getCategoryCode());
        category.setCategoryName(req.getCategoryName());
        category.setDescription(req.getDescription());
        category.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        category.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        category.setCorporateId(corporateId);
        category.setCreatedBy(username);
        category.setUpdatedBy(username);

        return categoryRepository.save(category);
    }

    @Transactional
    public ErrorCodeCategory updateCategory(Long id, ErrorCodeCategoryRequest req) {
        String username = getCurrentUsername();
        Long corporateId = getCurrentCorporateId();

        ErrorCodeCategory category = categoryRepository.findByIdAndCorporateId(id, corporateId)
            .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setCategoryCode(req.getCategoryCode());
        category.setCategoryName(req.getCategoryName());
        category.setDescription(req.getDescription());
        category.setDisplayOrder(req.getDisplayOrder());
        category.setIsActive(req.getIsActive());
        category.setUpdatedBy(username);

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Long corporateId = getCurrentCorporateId();
        ErrorCodeCategory category = categoryRepository.findByIdAndCorporateId(id, corporateId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.delete(category);
    }

    public List<ErrorCodeCategory> getAllCategories(Boolean activeOnly) {
        Long corporateId = getCurrentCorporateId();
        if (activeOnly != null && activeOnly) {
            return categoryRepository.findByCorporateIdAndIsActiveOrderByDisplayOrder(corporateId, true);
        }
        return categoryRepository.findByCorporateIdOrderByDisplayOrder(corporateId);
    }

    // ==================== VERSION & AUDIT ====================

    public List<ErrorCodeVersion> getVersionHistory(Long errorCodeId) {
        return versionRepository.findByErrorCodeIdOrderByVersionNumberDesc(errorCodeId);
    }

    public List<ErrorCodeAudit> getAuditLog(Long errorCodeId) {
        return auditRepository.findByErrorCodeIdOrderByChangedAtDesc(errorCodeId);
    }

    @Transactional
    public ErrorCodeDTO restoreVersion(Long errorCodeId, Integer versionNumber) {
        String username = getCurrentUsername();
        
        ErrorCodeVersion version = versionRepository.findByErrorCodeIdAndVersionNumber(errorCodeId, versionNumber)
            .orElseThrow(() -> new RuntimeException("Version not found"));

        ErrorCode errorCode = errorCodeRepository.findById(errorCodeId)
            .orElseThrow(() -> new RuntimeException("Error code not found"));

        errorCode.setErrorCode(version.getErrorCode());
        
        // Set App entity from version's appName
        if (version.getAppName() != null && !version.getAppName().isBlank()) {
            Long corporateId = getCurrentCorporateId();
            appRepository.findByCorporateIdAndName(corporateId, version.getAppName())
                .ifPresent(errorCode::setApp);
        }
        
        errorCode.setModuleName(version.getModuleName());
        errorCode.setSeverity(ErrorCode.ErrorSeverity.valueOf(version.getSeverity()));
        errorCode.setStatus(ErrorCode.ErrorStatus.valueOf(version.getStatus()));
        errorCode.setHttpStatusCode(version.getHttpStatusCode());
        errorCode.setIsPublic(version.getIsPublic());
        errorCode.setIsRetryable(version.getIsRetryable());
        errorCode.setDefaultMessage(version.getDefaultMessage());
        errorCode.setTechnicalDetails(version.getTechnicalDetails());
        errorCode.setResolutionSteps(version.getResolutionSteps());
        errorCode.setDocumentationUrl(version.getDocumentationUrl());
        errorCode.setUpdatedBy(username);

        errorCode = errorCodeRepository.save(errorCode);

        int latestVersion = versionRepository.findMaxVersionByErrorCodeId(errorCodeId).orElse(0);
        createVersion(errorCode, latestVersion + 1, "Restored from version " + versionNumber, username);

        logAudit(errorCodeId, "RESTORE", "version", String.valueOf(versionNumber), "current", username);

        return toDTO(errorCode);
    }

    // ==================== HELPER METHODS ====================

    private void createVersion(ErrorCode errorCode, int versionNumber, String changeDescription, String username) {
        ErrorCodeVersion version = new ErrorCodeVersion();
        version.setErrorCodeId(errorCode.getId());
        version.setVersionNumber(versionNumber);
        version.setErrorCode(errorCode.getErrorCode());
        version.setCategoryId(errorCode.getCategory() != null ? errorCode.getCategory().getId() : null);
        version.setAppName(errorCode.getApp() != null ? errorCode.getApp().getName() : null);
        version.setModuleName(errorCode.getModuleName());
        version.setSeverity(errorCode.getSeverity().name());
        version.setStatus(errorCode.getStatus().name());
        version.setHttpStatusCode(errorCode.getHttpStatusCode());
        version.setIsPublic(errorCode.getIsPublic());
        version.setIsRetryable(errorCode.getIsRetryable());
        version.setDefaultMessage(errorCode.getDefaultMessage());
        version.setTechnicalDetails(errorCode.getTechnicalDetails());
        version.setResolutionSteps(errorCode.getResolutionSteps());
        version.setDocumentationUrl(errorCode.getDocumentationUrl());
        version.setChangeDescription(changeDescription);
        version.setCreatedBy(username);
        versionRepository.save(version);
    }

    private void logAudit(Long errorCodeId, String action, String fieldName, String oldValue, String newValue, String username) {
        ErrorCodeAudit audit = new ErrorCodeAudit();
        audit.setErrorCodeId(errorCodeId);
        audit.setAction(action);
        audit.setFieldName(fieldName);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setChangedBy(username);
        audit.setIpAddress(getClientIp());
        audit.setUserAgent(request.getHeader("User-Agent"));
        auditRepository.save(audit);
    }

    private ErrorCodeDTO toDTO(ErrorCode errorCode) {
        ErrorCodeDTO dto = new ErrorCodeDTO();
        dto.setId(errorCode.getId());
        dto.setErrorCode(errorCode.getErrorCode());
        dto.setCategoryId(errorCode.getCategory() != null ? errorCode.getCategory().getId() : null);
        dto.setCategoryName(errorCode.getCategory() != null ? errorCode.getCategory().getCategoryName() : null);
        dto.setAppName(errorCode.getApp() != null ? errorCode.getApp().getName() : null);
        dto.setModuleName(errorCode.getModuleName());
        dto.setSeverity(errorCode.getSeverity());
        dto.setStatus(errorCode.getStatus());
        dto.setHttpStatusCode(errorCode.getHttpStatusCode());
        dto.setIsPublic(errorCode.getIsPublic());
        dto.setIsRetryable(errorCode.getIsRetryable());
        dto.setDefaultMessage(errorCode.getDefaultMessage());
        dto.setTechnicalDetails(errorCode.getTechnicalDetails());
        dto.setResolutionSteps(errorCode.getResolutionSteps());
        dto.setDocumentationUrl(errorCode.getDocumentationUrl());
        dto.setCreatedAt(errorCode.getCreatedAt());
        dto.setUpdatedAt(errorCode.getUpdatedAt());
        dto.setCreatedBy(errorCode.getCreatedBy());
        dto.setUpdatedBy(errorCode.getUpdatedBy());

        // Load translations
        List<ErrorCodeTranslation> translations = translationRepository.findByErrorCodeId(errorCode.getId());
        Map<String, ErrorCodeDTO.TranslationData> translationMap = translations.stream()
            .collect(Collectors.toMap(
                ErrorCodeTranslation::getLanguageCode,
                t -> {
                    ErrorCodeDTO.TranslationData data = new ErrorCodeDTO.TranslationData();
                    data.setId(t.getId());
                    data.setMessage(t.getMessage());
                    data.setTechnicalDetails(t.getTechnicalDetails());
                    data.setResolutionSteps(t.getResolutionSteps());
                    return data;
                }
            ));
        dto.setTranslations(translationMap);

        return dto;
    }

    // ==================== ERROR CODE SETTINGS & AUTO-GENERATION ====================

    public ErrorCodeSettings getErrorCodeSettings(String appName) {
        Long corporateId = getCurrentCorporateId();
        App app = appRepository.findByCorporateIdAndName(corporateId, appName)
            .orElseThrow(() -> new RuntimeException("App not found: " + appName));
        
        return settingsRepository.findByAppId(app.getId())
            .orElseGet(() -> createDefaultSettings(app));
    }

    @Transactional
    public ErrorCodeSettings updateErrorCodeSettings(String appName, ErrorCodeSettingsRequest request) {
        if (!request.isValid()) {
            throw new RuntimeException("Invalid settings request");
        }

        Long corporateId = getCurrentCorporateId();
        String username = getCurrentUsername();
        
        App app = appRepository.findByCorporateIdAndName(corporateId, appName)
            .orElseThrow(() -> new RuntimeException("App not found: " + appName));

        ErrorCodeSettings settings = settingsRepository.findByAppId(app.getId())
            .orElseGet(() -> createDefaultSettings(app));

        settings.setPrefix(request.getPrefix().trim().toUpperCase());
        settings.setSequenceLength(request.getSequenceLength());
        settings.setSeparator(request.getSeparator() != null ? request.getSeparator() : "");
        settings.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        settings.setUpdatedBy(username);

        return settingsRepository.save(settings);
    }

    @Transactional
    public ErrorCodeGenerationResponse generateNextErrorCode(String appName) {
        Long corporateId = getCurrentCorporateId();
        
        App app = appRepository.findByCorporateIdAndName(corporateId, appName)
            .orElseThrow(() -> new RuntimeException("App not found: " + appName));
        
        ErrorCodeSettings settings = settingsRepository.findByAppId(app.getId())
            .orElseGet(() -> createDefaultSettings(app));

        // Increment sequence
        settings.setCurrentSequence(settings.getCurrentSequence() + 1);
        settings = settingsRepository.save(settings);

        // Generate the code
        String generatedCode = generateCodeString(settings);

        return new ErrorCodeGenerationResponse(
            generatedCode,
            settings.getCurrentSequence(),
            settings.getPrefix(),
            settings.getSeparator(),
            settings.getSequenceLength()
        );
    }

    public ErrorCodeGenerationResponse previewNextErrorCode(String appName) {
        Long corporateId = getCurrentCorporateId();
        
        App app = appRepository.findByCorporateIdAndName(corporateId, appName)
            .orElseThrow(() -> new RuntimeException("App not found: " + appName));
        
        ErrorCodeSettings settings = settingsRepository.findByAppId(app.getId())
            .orElseGet(() -> createDefaultSettings(app));

        // Preview without incrementing
        Long nextSequence = settings.getCurrentSequence() + 1;
        ErrorCodeSettings previewSettings = new ErrorCodeSettings();
        previewSettings.setPrefix(settings.getPrefix());
        previewSettings.setSeparator(settings.getSeparator());
        previewSettings.setSequenceLength(settings.getSequenceLength());
        previewSettings.setCurrentSequence(nextSequence);

        String previewCode = generateCodeString(previewSettings);

        return new ErrorCodeGenerationResponse(
            previewCode,
            nextSequence,
            settings.getPrefix(),
            settings.getSeparator(),
            settings.getSequenceLength()
        );
    }

    public boolean isErrorCodeUnique(String appName, String errorCode) {
        Long corporateId = getCurrentCorporateId();
        
        App app = appRepository.findByCorporateIdAndName(corporateId, appName)
            .orElseThrow(() -> new RuntimeException("App not found: " + appName));
        
        return !errorCodeRepository.existsByErrorCodeAndAppId(errorCode, app.getId());
    }

    private ErrorCodeSettings createDefaultSettings(App app) {
        ErrorCodeSettings settings = new ErrorCodeSettings();
        settings.setApp(app);
        settings.setPrefix("E");
        settings.setSequenceLength(6);
        settings.setCurrentSequence(0L);
        settings.setSeparator("");
        settings.setIsActive(true);
        settings.setCreatedBy(getCurrentUsername());
        settings.setUpdatedBy(getCurrentUsername());
        return settingsRepository.save(settings);
    }

    private String generateCodeString(ErrorCodeSettings settings) {
        String format = "%s%s%0" + settings.getSequenceLength() + "d";
        return String.format(format, 
            settings.getPrefix(), 
            settings.getSeparator(), 
            settings.getCurrentSequence());
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private Long getCurrentCorporateId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.platform.security.UserPrincipal) {
            com.platform.security.UserPrincipal userPrincipal = (com.platform.security.UserPrincipal) auth.getPrincipal();
            return userPrincipal.getCorporateId();
        }
        return null; // Return null for global/system error codes or non-authenticated requests
    }

    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
