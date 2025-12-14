package com.platform.controller;

import com.platform.dto.AppDTO;
import com.platform.dto.AppRequest;
import com.platform.security.UserPrincipal;
import com.platform.service.AppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppController {
    
    private final AppService appService;
    
    /**
     * Get corporate ID from authenticated user
     */
    private Long getCorporateId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long corporateId = userPrincipal.getCorporateId();
            
            // Enhanced logging for debugging
            System.out.println("Authentication Principal: " + userPrincipal.getClass().getSimpleName());
            System.out.println("User Email: " + userPrincipal.getEmail());
            System.out.println("User ID: " + userPrincipal.getId());
            System.out.println("Corporate ID from Principal: " + corporateId);
            
            if (corporateId == null) {
                System.err.println("ERROR: User " + userPrincipal.getEmail() + " is not associated with any organization!");
                throw new RuntimeException("User is not associated with any organization");
            }
            
            return corporateId;
        }
        
        System.err.println("ERROR: No valid authentication found or principal is not UserPrincipal");
        if (authentication != null) {
            System.err.println("Authentication type: " + authentication.getClass().getSimpleName());
            System.err.println("Principal type: " + authentication.getPrincipal().getClass().getSimpleName());
        }
        
        throw new RuntimeException("Authentication required");
    }
    
    /**
     * Get user ID from authenticated user
     */
    private Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();
        }
        
        throw new RuntimeException("Authentication required");
    }
    
    /**
     * Get all apps for the authenticated user's corporate
     */
    @GetMapping
    public ResponseEntity<List<AppDTO>> getAllApps(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        Long corporateId = getCorporateId();
        Long userId = getUserId();
        
        // Enhanced logging for debugging corporate isolation
        System.out.println("=== APP REQUEST DEBUG ===");
        System.out.println("User ID: " + userId);
        System.out.println("Corporate ID: " + corporateId);
        System.out.println("Active Only: " + activeOnly);
        
        List<AppDTO> apps = activeOnly 
            ? appService.getActiveApps(corporateId)
            : appService.getAllApps(corporateId);
        
        System.out.println("Apps returned: " + apps.size());
        if (!apps.isEmpty()) {
            System.out.println("Corporate IDs in response: " + 
                apps.stream().map(AppDTO::getCorporateId).distinct().toList());
        }
        System.out.println("========================");
        
        return ResponseEntity.ok(apps);
    }
    
    /**
     * Get app by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppDTO> getAppById(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        AppDTO app = appService.getAppById(id, corporateId);
        return ResponseEntity.ok(app);
    }
    
    /**
     * Get app by app key
     */
    @GetMapping("/key/{appKey}")
    public ResponseEntity<AppDTO> getAppByKey(@PathVariable String appKey) {
        AppDTO app = appService.getAppByKey(appKey);
        
        // Verify app belongs to user's corporate
        Long corporateId = getCorporateId();
        if (!app.getCorporateId().equals(corporateId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(app);
    }
    
    /**
     * Create new app
     */
    @PostMapping
    public ResponseEntity<AppDTO> createApp(@Valid @RequestBody AppRequest request) {
        Long corporateId = getCorporateId();
        Long userId = getUserId();
        
        AppDTO app = appService.createApp(request, corporateId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(app);
    }
    
    /**
     * Update app
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppDTO> updateApp(
            @PathVariable Long id,
            @Valid @RequestBody AppRequest request) {
        Long corporateId = getCorporateId();
        AppDTO app = appService.updateApp(id, request, corporateId);
        return ResponseEntity.ok(app);
    }
    
    /**
     * Archive app (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveApp(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        appService.archiveApp(id, corporateId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Restore archived app
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<AppDTO> restoreApp(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        AppDTO app = appService.restoreApp(id, corporateId);
        return ResponseEntity.ok(app);
    }
    
    /**
     * Permanently delete app (admin only)
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deleteAppPermanently(@PathVariable Long id) {
        Long corporateId = getCorporateId();
        appService.deleteApp(id, corporateId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Search apps
     */
    @GetMapping("/search")
    public ResponseEntity<List<AppDTO>> searchApps(@RequestParam String query) {
        Long corporateId = getCorporateId();
        List<AppDTO> apps = appService.searchApps(corporateId, query);
        return ResponseEntity.ok(apps);
    }
    
    /**
     * Get app count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getAppCount(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        Long corporateId = getCorporateId();
        
        long count = activeOnly 
            ? appService.countActiveApps(corporateId)
            : appService.countApps(corporateId);
        
        return ResponseEntity.ok(count);
    }
}
