package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.dto.LovRequest;
import com.platform.dto.LovTypeDTO;
import com.platform.dto.LovWithTranslationsDTO;
import com.platform.entity.Lov;
import com.platform.entity.LovAudit;
import com.platform.entity.LovVersion;
import com.platform.repository.AppRepository;
import com.platform.repository.LovAuditRepository;
import com.platform.repository.LovRepository;
import com.platform.repository.LovVersionRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LovService {
    
    private final LovRepository lovRepository;
    private final LovVersionRepository lovVersionRepository;
    private final LovAuditRepository lovAuditRepository;
    private final AppRepository appRepository;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;
    
    public List<Lov> getAllLovs(Long corporateId, String lovType, Boolean active, String appName) {
        boolean hasApp = appName != null && !appName.isBlank();
        if (hasApp) {
            if (lovType != null && active != null) {
                return lovRepository.findByCorporateIdAndAppNameAndLovTypeAndActive(corporateId, appName, lovType, active);
            } else if (lovType != null) {
                return lovRepository.findByCorporateIdAndAppNameAndLovType(corporateId, appName, lovType);
            } else if (active != null) {
                return lovRepository.findByCorporateIdAndAppNameAndActive(corporateId, appName, active);
            } else {
                return lovRepository.findByCorporateIdAndAppName(corporateId, appName);
            }
        } else {
            if (lovType != null && active != null) {
                return lovRepository.findByCorporateIdAndLovTypeAndActive(corporateId, lovType, active);
            } else if (lovType != null) {
                return lovRepository.findByCorporateIdAndLovType(corporateId, lovType);
            } else if (active != null) {
                return lovRepository.findByCorporateIdAndActive(corporateId, active);
            } else {
                return lovRepository.findByCorporateId(corporateId);
            }
        }
    }
    
    public Lov getLovById(Long id, Long corporateId) {
        Lov lov = lovRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        if (!lov.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        logAudit(lov.getId(), LovAudit.Action.VIEW, null);
        return lov;
    }
    
    public Lov getLovByCode(String lovCode, Long corporateId) {
        Lov lov = lovRepository.findByLovCodeAndCorporateId(lovCode, corporateId)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        logAudit(lov.getId(), LovAudit.Action.VIEW, null);
        return lov;
    }
    
    public List<LovTypeDTO> getAllLovTypes(Long corporateId) {
        List<String> types = lovRepository.findDistinctLovTypesByCorporateId(corporateId);
        
        // If no types exist, return predefined types
        if (types.isEmpty()) {
            return getPredefinedLovTypes();
        }
        
        return types.stream()
            .map(type -> {
                Long count = lovRepository.countByLovTypeAndCorporateId(type, corporateId);
                return new LovTypeDTO(
                    type,
                    formatTypeName(type),
                    "LOV type: " + type,
                    true,
                    count
                );
            })
            .collect(Collectors.toList());
    }
    
    private List<LovTypeDTO> getPredefinedLovTypes() {
        return List.of(
            new LovTypeDTO("COUNTRY", "Country", "Country codes and names", true, 0L),
            new LovTypeDTO("MARKET_STATUS", "Market Status", "Market status values", true, 0L),
            new LovTypeDTO("USER_ROLE", "User Role", "User role types", true, 0L),
            new LovTypeDTO("CURRENCY", "Currency", "Currency codes", true, 0L),
            new LovTypeDTO("LANGUAGE", "Language", "Language codes", true, 0L),
            new LovTypeDTO("STATUS", "Status", "General status values", true, 0L),
            new LovTypeDTO("PRIORITY", "Priority", "Priority levels", true, 0L),
            new LovTypeDTO("CATEGORY", "Category", "Category types", true, 0L)
        );
    }
    
    @Transactional
    public Lov createLov(LovRequest request, Long corporateId) {
        // Note: lovCode is no longer unique - multiple values can have the same lovCode
        // lovCode represents the LOV type, and each value is a separate entry
        
        Lov lov = new Lov();
        lov.setLovCode(request.getLovCode());
        lov.setLovType(request.getLovType());
        lov.setLovValue(request.getLovValue());
        lov.setAttribute1(request.getAttribute1());
        lov.setAttribute2(request.getAttribute2());
        lov.setAttribute3(request.getAttribute3());
        lov.setDisplayOrder(request.getDisplayOrder());
        lov.setActive(request.getActive());
        lov.setParentLovId(request.getParentLovId());
        lov.setTranslationApp(request.getTranslationApp());
        lov.setTranslationKey(request.getTranslationKey());
        
        // Set App entity instead of appName
        if (request.getAppName() != null && !request.getAppName().isBlank()) {
            appRepository.findByCorporateIdAndName(corporateId, request.getAppName())
                .ifPresent(lov::setApp);
        }
        
        lov.setMetadata(request.getMetadata());
        lov.setCorporateId(corporateId);
        lov.setVersion(1);
        lov.setCreatedBy(getCurrentUsername());
        lov.setUpdatedBy(getCurrentUsername());
        
        Lov savedLov = lovRepository.save(lov);
        
        // Create version
        createVersion(savedLov, LovVersion.ChangeType.CREATE, "Initial creation");
        
        // Log audit
        logAudit(savedLov.getId(), LovAudit.Action.CREATE, null);
        
        return savedLov;
    }
    
    @Transactional
    public Lov updateLov(Long id, LovRequest request, Long corporateId) {
        Lov lov = lovRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        if (!lov.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        // Track changes
        Map<String, Object> changes = new HashMap<>();
        if (lov.getLovValue() != null && !lov.getLovValue().equals(request.getLovValue())) {
            changes.put("lovValue", Map.of("old", lov.getLovValue(), "new", request.getLovValue()));
        }
        if (!lov.getActive().equals(request.getActive())) {
            changes.put("active", Map.of("old", lov.getActive(), "new", request.getActive()));
        }
        
        // Update fields
        lov.setLovCode(request.getLovCode());
        lov.setLovType(request.getLovType());
        lov.setLovValue(request.getLovValue());
        lov.setAttribute1(request.getAttribute1());
        lov.setAttribute2(request.getAttribute2());
        lov.setAttribute3(request.getAttribute3());
        lov.setDisplayOrder(request.getDisplayOrder());
        lov.setActive(request.getActive());
        lov.setParentLovId(request.getParentLovId());
        lov.setTranslationApp(request.getTranslationApp());
        lov.setTranslationKey(request.getTranslationKey());
        
        // Update App entity instead of appName
        if (request.getAppName() != null && !request.getAppName().isBlank()) {
            appRepository.findByCorporateIdAndName(corporateId, request.getAppName())
                .ifPresent(lov::setApp);
        }
        
        lov.setMetadata(request.getMetadata());
        lov.setVersion(lov.getVersion() + 1);
        lov.setUpdatedBy(getCurrentUsername());
        
        Lov updatedLov = lovRepository.save(lov);
        
        // Create version
        createVersion(updatedLov, LovVersion.ChangeType.UPDATE, "Updated LOV");
        
        // Log audit
        try {
            String changesJson = objectMapper.writeValueAsString(changes);
            logAudit(updatedLov.getId(), LovAudit.Action.UPDATE, changesJson);
        } catch (Exception e) {
            logAudit(updatedLov.getId(), LovAudit.Action.UPDATE, null);
        }
        
        return updatedLov;
    }
    
    @Transactional
    public void deleteLov(Long id, Long corporateId) {
        Lov lov = lovRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        if (!lov.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        // Create version before deletion
        createVersion(lov, LovVersion.ChangeType.DELETE, "LOV deleted");
        
        // Log audit
        logAudit(lov.getId(), LovAudit.Action.DELETE, null);
        
        lovRepository.delete(lov);
    }
    
    public List<LovVersion> getLovVersions(Long lovId, Long corporateId) {
        Lov lov = lovRepository.findById(lovId)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        if (!lov.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        return lovVersionRepository.findByLovIdOrderByVersionDesc(lovId);
    }
    
    @Transactional
    public Lov restoreLovVersion(Long lovId, Long versionId, Long corporateId) {
        Lov lov = lovRepository.findById(lovId)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        if (!lov.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        LovVersion version = lovVersionRepository.findById(versionId)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        
        // Restore from version
        lov.setLovCode(version.getLovCode());
        lov.setLovType(version.getLovType());
        lov.setLovValue(version.getValue());
        lov.setDisplayOrder(version.getDisplayOrder());
        lov.setActive(version.getActive());
        lov.setTranslationKey(version.getTranslationKey());
        lov.setMetadata(version.getMetadata());
        lov.setVersion(lov.getVersion() + 1);
        lov.setUpdatedBy(getCurrentUsername());
        
        Lov restoredLov = lovRepository.save(lov);
        
        // Create new version
        createVersion(restoredLov, LovVersion.ChangeType.UPDATE, "Restored from version " + version.getVersion());
        
        // Log audit
        logAudit(restoredLov.getId(), LovAudit.Action.UPDATE, "Restored from version " + version.getVersion());
        
        return restoredLov;
    }
    
    public List<LovAudit> getLovAudit(Long lovId, Long corporateId) {
        Lov lov = lovRepository.findById(lovId)
            .orElseThrow(() -> new RuntimeException("LOV not found"));
        
        if (!lov.getCorporateId().equals(corporateId)) {
            throw new RuntimeException("Access denied");
        }
        
        return lovAuditRepository.findByLovIdOrderByTimestampDesc(lovId);
    }
    
    private void createVersion(Lov lov, LovVersion.ChangeType changeType, String description) {
        LovVersion version = new LovVersion();
        version.setLovId(lov.getId());
        version.setVersion(lov.getVersion());
        version.setLovCode(lov.getLovCode());
        version.setLovType(lov.getLovType());
        version.setValue(lov.getLovValue());
        version.setDisplayOrder(lov.getDisplayOrder());
        version.setActive(lov.getActive());
        version.setTranslationKey(lov.getTranslationKey());
        version.setMetadata(lov.getMetadata());
        version.setChangedBy(getCurrentUsername());
        version.setChangedAt(LocalDateTime.now());
        version.setChangeType(changeType);
        version.setChangeDescription(description);
        
        lovVersionRepository.save(version);
    }
    
    private void logAudit(Long lovId, LovAudit.Action action, String changes) {
        LovAudit audit = new LovAudit();
        audit.setLovId(lovId);
        audit.setAction(action);
        audit.setUserId(getCurrentUserId());
        audit.setUserName(getCurrentUsername());
        audit.setTimestamp(LocalDateTime.now());
        audit.setIpAddress(getClientIpAddress());
        audit.setUserAgent(request.getHeader("User-Agent"));
        audit.setChanges(changes);
        
        lovAuditRepository.save(audit);
    }
    
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
    
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.platform.security.UserPrincipal) {
            com.platform.security.UserPrincipal userPrincipal = (com.platform.security.UserPrincipal) auth.getPrincipal();
            return userPrincipal.getId();
        }
        return null;
    }
    
    private Long getCurrentCorporateId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.platform.security.UserPrincipal) {
            com.platform.security.UserPrincipal userPrincipal = (com.platform.security.UserPrincipal) auth.getPrincipal();
            return userPrincipal.getCorporateId();
        }
        return null;
    }
    
    private String getClientIpAddress() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private String formatTypeName(String type) {
        String[] words = type.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    public List<LovWithTranslationsDTO> getAllLovsWithTranslations(Long corporateId, String lovType, Boolean active) {
        List<Lov> lovs = getAllLovs(corporateId, lovType, active, null);
        
        return lovs.stream()
            .map(lov -> {
                LovWithTranslationsDTO dto = new LovWithTranslationsDTO();
                dto.setId(lov.getId());
                dto.setLovCode(lov.getLovCode());
                dto.setLovType(lov.getLovType());
                dto.setLovValue(lov.getLovValue());
                dto.setAttribute1(lov.getAttribute1());
                dto.setAttribute2(lov.getAttribute2());
                dto.setAttribute3(lov.getAttribute3());
                dto.setDisplayOrder(lov.getDisplayOrder());
                dto.setActive(lov.getActive());
                dto.setParentLovId(lov.getParentLovId());
                dto.setTranslationApp(lov.getTranslationApp());
                dto.setTranslationKey(lov.getTranslationKey());
                dto.setMetadata(lov.getMetadata());
                dto.setCorporateId(lov.getCorporateId());
                dto.setVersion(lov.getVersion());
                dto.setCreatedBy(lov.getCreatedBy());
                dto.setCreatedAt(lov.getCreatedAt().toString());
                dto.setUpdatedBy(lov.getUpdatedBy());
                dto.setUpdatedAt(lov.getUpdatedAt().toString());
                
                // Fetch translations for this LOV
                Map<String, String> descriptions = fetchTranslations(lov.getTranslationApp(), lov.getTranslationKey());
                dto.setDescriptions(descriptions);
                
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    private Map<String, String> fetchTranslations(String app, String key) {
        // TODO: Integrate with your translation service
        // For now, return mock data
        Map<String, String> translations = new HashMap<>();
        
        if (key != null && !key.isEmpty()) {
            // Mock translations - replace with actual translation service call
            translations.put("en", key + " (English)");
            translations.put("ar", key + " (Arabic)");
            translations.put("fr", key + " (French)");
        }
        
        return translations;
    }
    
    public List<com.platform.dto.LovPageDTO> getAllLovPages(Long corporateId, Boolean active, String appName) {
        // Get all LOVs for this corporate
        List<Lov> allLovs;
        boolean hasApp = appName != null && !appName.isBlank();
        if (hasApp) {
            allLovs = active != null
                ? lovRepository.findByCorporateIdAndAppNameAndActive(corporateId, appName, active)
                : lovRepository.findByCorporateIdAndAppName(corporateId, appName);
        } else {
            allLovs = active != null
                ? lovRepository.findByCorporateIdAndActive(corporateId, active)
                : lovRepository.findByCorporateId(corporateId);
        }
        
        // Group by lovCode
        Map<String, List<Lov>> groupedByCode = allLovs.stream()
            .collect(Collectors.groupingBy(Lov::getLovCode));
        
        // Create page DTOs
        return groupedByCode.entrySet().stream()
            .map(entry -> {
                String lovCode = entry.getKey();
                List<Lov> values = entry.getValue();
                
                // Get the first value for metadata
                Lov firstValue = values.get(0);
                
                // Parse metadata to get name and description
                String name = lovCode;
                String description = "";
                if (firstValue.getMetadata() != null && !firstValue.getMetadata().isEmpty()) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(
                            firstValue.getMetadata(), 
                            Map.class
                        );
                        name = (String) metadata.getOrDefault("name", lovCode);
                        description = (String) metadata.getOrDefault("description", "");
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
                
                com.platform.dto.LovPageDTO page = new com.platform.dto.LovPageDTO();
                page.setLovCode(lovCode);
                page.setName(name);
                page.setDescription(description);
                page.setTranslationApp(firstValue.getTranslationApp());
                page.setValueCount((long) values.size());
                page.setActive(values.stream().anyMatch(Lov::getActive));
                page.setCreatedBy(firstValue.getCreatedBy());
                page.setCreatedAt(firstValue.getCreatedAt().toString());
                page.setUpdatedBy(firstValue.getUpdatedBy());
                page.setUpdatedAt(firstValue.getUpdatedAt().toString());
                
                return page;
            })
            .sorted((a, b) -> a.getLovCode().compareTo(b.getLovCode()))
            .collect(Collectors.toList());
    }

    public List<LovWithTranslationsDTO> getAllLovsWithTranslations(Long corporateId, String lovType, Boolean active, String appName) {
        List<Lov> lovs = getAllLovs(corporateId, lovType, active, appName);
        return lovs.stream()
            .map(lov -> {
                LovWithTranslationsDTO dto = new LovWithTranslationsDTO();
                dto.setId(lov.getId());
                dto.setLovCode(lov.getLovCode());
                dto.setLovType(lov.getLovType());
                dto.setLovValue(lov.getLovValue());
                dto.setAttribute1(lov.getAttribute1());
                dto.setAttribute2(lov.getAttribute2());
                dto.setAttribute3(lov.getAttribute3());
                dto.setDisplayOrder(lov.getDisplayOrder());
                dto.setActive(lov.getActive());
                dto.setParentLovId(lov.getParentLovId());
                dto.setTranslationApp(lov.getTranslationApp());
                dto.setTranslationKey(lov.getTranslationKey());
                dto.setMetadata(lov.getMetadata());
                dto.setCorporateId(lov.getCorporateId());
                dto.setVersion(lov.getVersion());
                dto.setCreatedBy(lov.getCreatedBy());
                dto.setCreatedAt(lov.getCreatedAt().toString());
                dto.setUpdatedBy(lov.getUpdatedBy());
                dto.setUpdatedAt(lov.getUpdatedAt().toString());
                Map<String, String> descriptions = fetchTranslations(lov.getTranslationApp(), lov.getTranslationKey());
                dto.setDescriptions(descriptions);
                return dto;
            })
            .collect(Collectors.toList());
    }
}
