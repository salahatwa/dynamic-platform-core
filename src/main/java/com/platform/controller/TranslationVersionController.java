package com.platform.controller;

import com.platform.entity.TranslationApp;
import com.platform.entity.TranslationVersion;
import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TranslationAppService;
import com.platform.service.TranslationVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/translation-versions")
@RequiredArgsConstructor
@Tag(name = "Translation Versions", description = "Translation version management")
public class TranslationVersionController {
    
    private final TranslationVersionService versionService;
    private final TranslationAppService appService;
    private final UserRepository userRepository;
    
    @GetMapping
    @Operation(summary = "Get all versions for an app")
    public ResponseEntity<?> getAllVersions(
            @RequestParam Long appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        if (!appService.belongsToCorporate(appId, currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "version"));
        Page<TranslationVersion> versions = versionService.getAllByApp(appId, pageable);
        
        return ResponseEntity.ok(versions);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get version by ID")
    public ResponseEntity<?> getVersion(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationVersion version = versionService.getById(id)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        
        if (!version.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        return ResponseEntity.ok(version);
    }
    
    @GetMapping("/{id}/snapshot")
    @Operation(summary = "Get version snapshot")
    public ResponseEntity<?> getVersionSnapshot(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationVersion version = versionService.getById(id)
            .orElseThrow(() -> new RuntimeException("Version not found"));
        
        if (!version.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        Map<String, Object> snapshot = versionService.getVersionSnapshot(id);
        return ResponseEntity.ok(snapshot);
    }
    
    @PostMapping
    @Operation(summary = "Create new version")
    public ResponseEntity<?> createVersion(@RequestBody Map<String, Object> request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Long appId = Long.valueOf(request.get("appId").toString());
        String changelog = (String) request.get("changelog");
        
        TranslationApp app = appService.getById(appId)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        TranslationVersion version = versionService.createVersion(app, changelog, currentUser);
        return ResponseEntity.ok(version);
    }
    
    private User getCurrentUserWithCorporate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userRepository.findByIdWithCorporate(userPrincipal.getId()).orElse(null);
        }
        return null;
    }
}
