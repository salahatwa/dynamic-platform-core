package com.platform.controller;

import com.platform.dto.TranslationAppRequest;
import com.platform.dto.TranslationAppResponse;
import com.platform.entity.TranslationApp;
import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TranslationAppService;
import com.platform.service.TranslationKeyService;
import com.platform.service.TranslationService;

import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translation-apps")
@RequiredArgsConstructor
@Tag(name = "Translation Apps", description = "Translation application management")
public class TranslationAppController {
    
    private final TranslationAppService appService;
    private final TranslationKeyService keyService;
    private final TranslationService translationService;
    private final UserRepository userRepository;
    
    @GetMapping
    @Operation(summary = "Get all translation apps")
    public ResponseEntity<?> getAllApps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<TranslationApp> apps;
        if (search != null && !search.isEmpty()) {
            apps = appService.searchByCorporate(currentUser.getCorporate().getId(), search, pageable);
        } else {
            apps = appService.getAllByCorporate(currentUser.getCorporate().getId(), pageable);
        }
        
        Page<TranslationAppResponse> response = apps.map(this::toResponse);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get translation app by ID")
    public ResponseEntity<?> getApp(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        return ResponseEntity.ok(toResponse(app));
    }
    
    @PostMapping("/get-or-create")
    @Operation(summary = "Get or create translation app by name")
    public ResponseEntity<?> getOrCreateApp(@Valid @RequestBody TranslationAppRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        // Try to find existing app by name and corporate
        Optional<TranslationApp> existingApp = appService.getByNameAndCorporate(request.getName(), currentUser.getCorporate().getId());
        if (existingApp.isPresent()) {
            return ResponseEntity.ok(toResponse(existingApp.get()));
        }
        
        // Create new app if not found
        TranslationApp app = TranslationApp.builder()
            .name(request.getName())
            .description(request.getDescription())
            .defaultLanguage(request.getDefaultLanguage())
            .active(request.getActive())
            .build();
        
        app.setSupportedLanguagesSet(request.getSupportedLanguages());
        
        TranslationApp created = appService.create(app, currentUser.getCorporate());
        return ResponseEntity.ok(toResponse(created));
    }

    @PostMapping
    @Operation(summary = "Create translation app")
    public ResponseEntity<?> createApp(@Valid @RequestBody TranslationAppRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = TranslationApp.builder()
            .name(request.getName())
            .description(request.getDescription())
            .defaultLanguage(request.getDefaultLanguage())
            .active(request.getActive())
            .build();
        
        app.setSupportedLanguagesSet(request.getSupportedLanguages());
        
        TranslationApp created = appService.create(app, currentUser.getCorporate());
        return ResponseEntity.ok(toResponse(created));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update translation app")
    public ResponseEntity<?> updateApp(@PathVariable Long id, @Valid @RequestBody TranslationAppRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setDefaultLanguage(request.getDefaultLanguage());
        app.setSupportedLanguagesSet(request.getSupportedLanguages());
        app.setActive(request.getActive());
        
        TranslationApp updated = appService.update(app);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete translation app")
    public ResponseEntity<?> deleteApp(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        appService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/regenerate-api-key")
    @Operation(summary = "Regenerate API key")
    public ResponseEntity<?> regenerateApiKey(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        if (!appService.belongsToCorporate(id, currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        String newApiKey = appService.regenerateApiKey(id);
        return ResponseEntity.ok(java.util.Map.of("apiKey", newApiKey));
    }
    
    // Language Management Endpoints
    
    @GetMapping("/{id}/languages")
    @Operation(summary = "Get supported languages for translation app")
    public ResponseEntity<?> getAppLanguages(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        return ResponseEntity.ok(app.getSupportedLanguagesSet());
    }
    
    @PostMapping("/{id}/languages")
    @Operation(summary = "Add a new language to translation app")
    public ResponseEntity<?> addAppLanguage(@PathVariable Long id, @RequestBody java.util.Map<String, String> request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        String languageCode = request.get("languageCode");
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Language code is required");
        }
        
        languageCode = languageCode.toLowerCase().trim();
        
        // Check if language already exists
        if (app.getSupportedLanguagesSet().contains(languageCode)) {
            return ResponseEntity.badRequest().body("Language already exists");
        }
        
        // Add the new language
        java.util.Set<String> languages = new java.util.HashSet<>(app.getSupportedLanguagesSet());
        languages.add(languageCode);
        app.setSupportedLanguagesSet(languages);
        
        appService.update(app);
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}/languages/{languageCode}")
    @Operation(summary = "Remove a language from translation app")
    public ResponseEntity<?> removeAppLanguage(@PathVariable Long id, @PathVariable String languageCode) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        languageCode = languageCode.toLowerCase().trim();
        
        // Don't allow removing the default language
        if (languageCode.equals(app.getDefaultLanguage())) {
            return ResponseEntity.badRequest().body("Cannot remove the default language");
        }
        
        // Don't allow removing if it's the only language
        if (app.getSupportedLanguagesSet().size() <= 1) {
            return ResponseEntity.badRequest().body("Cannot remove the last remaining language");
        }
        
        // Check if language exists
        if (!app.getSupportedLanguagesSet().contains(languageCode)) {
            return ResponseEntity.badRequest().body("Language not found");
        }
        
        // Remove the language
        java.util.Set<String> languages = new java.util.HashSet<>(app.getSupportedLanguagesSet());
        languages.remove(languageCode);
        app.setSupportedLanguagesSet(languages);
        
        appService.update(app);
        
        // TODO: Optionally delete all translations for this language
        // translationService.deleteByAppAndLanguage(id, languageCode);
        
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/languages")
    @Operation(summary = "Update all supported languages for translation app")
    public ResponseEntity<?> updateAppLanguages(@PathVariable Long id, @RequestBody java.util.Map<String, java.util.Set<String>> request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        java.util.Set<String> languages = request.get("languages");
        if (languages == null || languages.isEmpty()) {
            return ResponseEntity.badRequest().body("Languages list is required and cannot be empty");
        }
        
        // Ensure default language is included
        if (!languages.contains(app.getDefaultLanguage())) {
            languages = new java.util.HashSet<>(languages);
            languages.add(app.getDefaultLanguage());
        }
        
        // Normalize language codes
        java.util.Set<String> normalizedLanguages = languages.stream()
            .map(lang -> lang.toLowerCase().trim())
            .collect(java.util.stream.Collectors.toSet());
        
        app.setSupportedLanguagesSet(normalizedLanguages);
        appService.update(app);
        
        return ResponseEntity.ok().build();
    }
    
    private TranslationAppResponse toResponse(TranslationApp app) {
        return TranslationAppResponse.builder()
            .id(app.getId())
            .name(app.getName())
            .description(app.getDescription())
            .apiKey(app.getApiKey())
            .defaultLanguage(app.getDefaultLanguage())
            .supportedLanguages(app.getSupportedLanguagesSet())
            .active(app.getActive())
            .keysCount(keyService.countByApp(app.getId()))
            .translationsCount(translationService.countByApp(app.getId()))
            .createdAt(app.getCreatedAt())
            .updatedAt(app.getUpdatedAt())
            .build();
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
