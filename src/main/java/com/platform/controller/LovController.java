package com.platform.controller;

import com.platform.dto.LovRequest;
import com.platform.dto.LovTypeDTO;
import com.platform.dto.LovWithTranslationsDTO;
import com.platform.entity.Lov;
import com.platform.entity.LovAudit;
import com.platform.entity.LovVersion;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.security.RequirePermission;
import com.platform.security.UserPrincipal;
import com.platform.service.LovService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lov")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LovController {
    
    private final LovService lovService;
    
    // Get corporate ID from authenticated user or API key
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
    
    @GetMapping
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<Lov>> getAllLovs(
            @RequestParam(required = false) String lovType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        List<Lov> lovs = lovService.getAllLovs(corporateId, lovType, active, appName);
        return ResponseEntity.ok(lovs);
    }
    
    @GetMapping("/pages")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<com.platform.dto.LovPageDTO>> getAllLovPages(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        List<com.platform.dto.LovPageDTO> pages = lovService.getAllLovPages(corporateId, active, appName);
        return ResponseEntity.ok(pages);
    }
    
    @GetMapping("/with-translations")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<LovWithTranslationsDTO>> getAllLovsWithTranslations(
            @RequestParam(required = false) String lovType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        List<LovWithTranslationsDTO> lovs = lovService.getAllLovsWithTranslations(corporateId, lovType, active, appName);
        return ResponseEntity.ok(lovs);
    }
    
    @GetMapping("/{id}")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<Lov> getLovById(@PathVariable Long id, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        Lov lov = lovService.getLovById(id, corporateId);
        return ResponseEntity.ok(lov);
    }
    
    @GetMapping("/code/{lovCode}")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<Lov> getLovByCode(@PathVariable String lovCode, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        Lov lov = lovService.getLovByCode(lovCode, corporateId);
        return ResponseEntity.ok(lov);
    }
    
    @GetMapping("/type/{lovType}")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<Lov>> getLovsByType(@PathVariable String lovType,
                                                   @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        List<Lov> lovs = lovService.getAllLovs(corporateId, lovType, null, appName);
        return ResponseEntity.ok(lovs);
    }
    
    @GetMapping("/types")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<LovTypeDTO>> getAllLovTypes(@RequestParam(required = false) String appName) {
        System.out.println("GET /api/lov/types called");
        Long corporateId = getCorporateId();
        System.out.println("Corporate ID: " + corporateId);
        List<LovTypeDTO> types = lovService.getAllLovTypes(corporateId);
        System.out.println("Returning " + types.size() + " LOV types");
        return ResponseEntity.ok(types);
    }
    
    @PostMapping
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.CREATE)
    public ResponseEntity<Lov> createLov(@Valid @RequestBody LovRequest request, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        Lov lov = lovService.createLov(request, corporateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(lov);
    }
    
    @PutMapping("/{id}")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.UPDATE)
    public ResponseEntity<Lov> updateLov(
            @PathVariable Long id,
            @Valid @RequestBody LovRequest request,
            @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        Lov lov = lovService.updateLov(id, request, corporateId);
        return ResponseEntity.ok(lov);
    }
    
    @DeleteMapping("/{id}")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.DELETE)
    public ResponseEntity<Void> deleteLov(@PathVariable Long id, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        lovService.deleteLov(id, corporateId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/versions")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<LovVersion>> getLovVersions(@PathVariable Long id, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        List<LovVersion> versions = lovService.getLovVersions(id, corporateId);
        return ResponseEntity.ok(versions);
    }
    
    @PostMapping("/{id}/versions/{versionId}/restore")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.UPDATE)
    public ResponseEntity<Lov> restoreLovVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        Lov lov = lovService.restoreLovVersion(id, versionId, corporateId);
        return ResponseEntity.ok(lov);
    }
    
    @GetMapping("/{id}/audit")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.READ)
    public ResponseEntity<List<LovAudit>> getLovAudit(@PathVariable Long id, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        List<LovAudit> audit = lovService.getLovAudit(id, corporateId);
        return ResponseEntity.ok(audit);
    }
    
    // Bulk Operations
    @PostMapping("/bulk")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.CREATE)
    public ResponseEntity<List<Lov>> bulkCreateLovs(@RequestBody List<LovRequest> requests, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        
        System.out.println("Bulk create received " + requests.size() + " requests");
        
        // Validate and set defaults
        for (int i = 0; i < requests.size(); i++) {
            LovRequest request = requests.get(i);
            System.out.println("Request " + i + ": lovCode=" + request.getLovCode() + ", lovType=" + request.getLovType());
            
            if (request.getLovCode() == null || request.getLovCode().trim().isEmpty()) {
                throw new RuntimeException("LOV code is required for entry " + (i + 1));
            }
            
            // If lovType is not set, use lovCode as lovType
            if (request.getLovType() == null || request.getLovType().trim().isEmpty()) {
                request.setLovType(request.getLovCode());
            }
            
            if (request.getDisplayOrder() == null) {
                request.setDisplayOrder(0);
            }
            if (request.getActive() == null) {
                request.setActive(true);
            }
            if (request.getTranslationApp() == null || request.getTranslationApp().trim().isEmpty()) {
                request.setTranslationApp("default");
            }
        }
        
        List<Lov> createdLovs = requests.stream()
            .map(request -> lovService.createLov(request, corporateId))
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLovs);
    }
    
    @PutMapping("/bulk")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.UPDATE)
    public ResponseEntity<List<Lov>> bulkUpdateLovs(@RequestBody List<BulkUpdateRequest> updates, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        
        // Validate basic requirements
        for (BulkUpdateRequest update : updates) {
            if (update.getId() == null) {
                throw new RuntimeException("ID is required for all updates");
            }
            if (update.getRequest().getDisplayOrder() == null) {
                update.getRequest().setDisplayOrder(0);
            }
            if (update.getRequest().getActive() == null) {
                update.getRequest().setActive(true);
            }
        }
        
        List<Lov> updatedLovs = updates.stream()
            .map(update -> lovService.updateLov(update.getId(), update.getRequest(), corporateId))
            .toList();
        return ResponseEntity.ok(updatedLovs);
    }
    
    @DeleteMapping("/bulk")
    @RequirePermission(resource = PermissionResource.LOV, action = PermissionAction.DELETE)
    public ResponseEntity<Void> bulkDeleteLovs(@RequestBody List<Long> ids, @RequestParam(required = false) String appName) {
        Long corporateId = getCorporateId();
        ids.forEach(id -> lovService.deleteLov(id, corporateId));
        return ResponseEntity.noContent().build();
    }
    
    // Helper class for bulk update
    public static class BulkUpdateRequest {
        private Long id;
        private LovRequest request;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public LovRequest getRequest() { return request; }
        public void setRequest(LovRequest request) { this.request = request; }
    }
}
