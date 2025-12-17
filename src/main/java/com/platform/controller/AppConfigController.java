package com.platform.controller;

import com.platform.dto.AppConfigDTO;
import com.platform.dto.AppConfigGroupRequest;
import com.platform.dto.AppConfigRequest;
import com.platform.entity.AppConfig;
import com.platform.entity.AppConfigAudit;
import com.platform.entity.AppConfigGroup;
import com.platform.entity.AppConfigVersion;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.security.RequirePermission;
import com.platform.service.AppConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppConfigController {
    
    private final AppConfigService configService;
    
    // Get corporate ID from authenticated user or API key
    private Long getCorporateId() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof com.platform.security.UserPrincipal) {
            com.platform.security.UserPrincipal userPrincipal = (com.platform.security.UserPrincipal) authentication.getPrincipal();
            Long corporateId = userPrincipal.getCorporateId();
            
            if (corporateId == null) {
                throw new RuntimeException("User is not associated with any organization");
            }
            
            return corporateId;
        }
        
        throw new RuntimeException("Authentication required");
    }
    
    // ==================== Configuration Endpoints ====================
    
    @GetMapping
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<List<AppConfigDTO>> getAllConfigs(
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long corporateId = getCorporateId();
        List<AppConfigDTO> configs = configService.getAllConfigsWithGroupNames(corporateId, appName, appId, groupId, active, page, size);
        return ResponseEntity.ok(configs);
    }
    
    @GetMapping("/{id}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<AppConfig> getConfigById(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        AppConfig config = configService.getConfigById(id, corporateId);
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/key/{configKey}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<AppConfig> getConfigByKey(
            @PathVariable String configKey,
            @RequestParam String appName) {
        Long corporateId = getCorporateId();
        AppConfig config = configService.getConfigByKey(configKey, appName, corporateId);
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/public/{appName}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<List<AppConfig>> getPublicConfigs(@PathVariable String appName) {
        Long corporateId = getCorporateId();
        List<AppConfig> configs = configService.getPublicConfigs(appName, corporateId);
        return ResponseEntity.ok(configs);
    }
    
    @PostMapping
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.CREATE)
    public ResponseEntity<AppConfig> createConfig(@Valid @RequestBody AppConfigRequest request) {
        Long corporateId = getCorporateId();
        AppConfig config = configService.createConfig(request, corporateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }
    
    @PutMapping("/{id}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.UPDATE)
    public ResponseEntity<AppConfig> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AppConfigRequest request) {
        Long corporateId = getCorporateId();
        AppConfig config = configService.updateConfig(id, request, corporateId);
        return ResponseEntity.ok(config);
    }
    
    @DeleteMapping("/{id}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.DELETE)
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        configService.deleteConfig(id, corporateId);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== Configuration Group Endpoints ====================
    
    @GetMapping("/groups")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<List<AppConfigGroup>> getAllGroups(
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) Boolean active) {
        Long corporateId = getCorporateId();
        List<AppConfigGroup> groups = configService.getAllGroups(corporateId, appName, appId, active);
        return ResponseEntity.ok(groups);
    }
    
    @GetMapping("/groups/{id}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<AppConfigGroup> getGroupById(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        AppConfigGroup group = configService.getGroupById(id, corporateId);
        return ResponseEntity.ok(group);
    }
    
    @PostMapping("/groups")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.CREATE)
    public ResponseEntity<AppConfigGroup> createGroup(@Valid @RequestBody AppConfigGroupRequest request) {
        Long corporateId = getCorporateId();
        AppConfigGroup group = configService.createGroup(request, corporateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }
    
    @PutMapping("/groups/{id}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.UPDATE)
    public ResponseEntity<AppConfigGroup> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody AppConfigGroupRequest request) {
        Long corporateId = getCorporateId();
        AppConfigGroup group = configService.updateGroup(id, request, corporateId);
        return ResponseEntity.ok(group);
    }
    
    @DeleteMapping("/groups/{id}")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.DELETE)
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        configService.deleteGroup(id, corporateId);
        return ResponseEntity.noContent().build();
    }
    
    // ==================== Versioning Endpoints ====================
    
    @GetMapping("/{id}/versions")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<List<AppConfigVersion>> getConfigVersions(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        List<AppConfigVersion> versions = configService.getConfigVersions(id, corporateId);
        return ResponseEntity.ok(versions);
    }
    
    @PostMapping("/{id}/versions/{versionId}/restore")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.UPDATE)
    public ResponseEntity<AppConfig> restoreVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        Long corporateId = getCorporateId();
        AppConfig config = configService.restoreVersion(id, versionId, corporateId);
        return ResponseEntity.ok(config);
    }
    
    // ==================== Audit Endpoints ====================
    
    @GetMapping("/{id}/audit")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<List<AppConfigAudit>> getConfigAudit(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        List<AppConfigAudit> audit = configService.getConfigAudit(id, corporateId);
        return ResponseEntity.ok(audit);
    }
    
    // ==================== Utility Endpoints ====================
    
    @GetMapping("/apps")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.READ)
    public ResponseEntity<List<String>> getAppNames() {
        Long corporateId = getCorporateId();
        List<String> appNames = configService.getAppNames(corporateId);
        return ResponseEntity.ok(appNames);
    }
    
    @PostMapping("/cache/invalidate")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.UPDATE)
    public ResponseEntity<Void> invalidateCache() {
        configService.invalidateCache();
        return ResponseEntity.ok().build();
    }
    
    // ==================== Bulk Operations ====================
    
    @PostMapping("/bulk")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.CREATE)
    public ResponseEntity<List<AppConfig>> bulkCreateConfigs(@RequestBody List<AppConfigRequest> requests) {
        Long corporateId = getCorporateId();
        
        List<AppConfig> createdConfigs = requests.stream()
            .map(request -> configService.createConfig(request, corporateId))
            .toList();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdConfigs);
    }
    
    @DeleteMapping("/bulk")
    @RequirePermission(resource = PermissionResource.APP_CONFIG, action = PermissionAction.DELETE)
    public ResponseEntity<Void> bulkDeleteConfigs(@RequestBody List<Long> ids) {
        Long corporateId = getCorporateId();
        ids.forEach(id -> configService.deleteConfig(id, corporateId));
        return ResponseEntity.noContent().build();
    }
}
