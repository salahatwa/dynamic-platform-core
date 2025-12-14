package com.platform.controller;

import com.platform.dto.TranslationKeyRequest;
import com.platform.dto.TranslationKeyResponse;
import com.platform.dto.TranslationValueResponse;
import com.platform.entity.TranslationApp;
import com.platform.entity.TranslationKey;
import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.TranslationAppService;
import com.platform.service.TranslationKeyService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/translation-keys")
@RequiredArgsConstructor
@Tag(name = "Translation Keys", description = "Translation key management")
public class TranslationKeyController {
    
    private final TranslationKeyService keyService;
    private final TranslationAppService appService;
    private final UserRepository userRepository;
    
    @GetMapping
    @Operation(summary = "Get all translation keys for an app")
    public ResponseEntity<?> getAllKeys(
            @RequestParam Long appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        if (!appService.belongsToCorporate(appId, currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "keyName"));
        
        Page<TranslationKey> keys;
        if (search != null && !search.isEmpty()) {
            keys = keyService.searchByApp(appId, search, pageable);
        } else {
            keys = keyService.getAllByApp(appId, pageable);
        }
        
        Page<TranslationKeyResponse> response = keys.map(this::toResponse);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get translation key by ID")
    public ResponseEntity<?> getKey(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationKey key = keyService.getByIdWithTranslations(id)
            .orElseThrow(() -> new RuntimeException("Key not found"));
        
        if (!key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Key belongs to another organization");
        }
        
        return ResponseEntity.ok(toResponse(key));
    }
    
    @PostMapping
    @Operation(summary = "Create translation key")
    public ResponseEntity<?> createKey(@Valid @RequestBody TranslationKeyRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationApp app = appService.getById(request.getAppId())
            .orElseThrow(() -> new RuntimeException("App not found"));
        
        if (!app.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: App belongs to another organization");
        }
        
        if (keyService.existsByAppAndKeyName(request.getAppId(), request.getKeyName())) {
            return ResponseEntity.badRequest().body("Key name already exists in this app");
        }
        
        TranslationKey key = TranslationKey.builder()
            .app(app)
            .keyName(request.getKeyName())
            .description(request.getDescription())
            .context(request.getContext())
            .build();
        
        TranslationKey created = keyService.create(key, currentUser);
        return ResponseEntity.ok(toResponse(created));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update translation key")
    public ResponseEntity<?> updateKey(@PathVariable Long id, @Valid @RequestBody TranslationKeyRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationKey key = keyService.getById(id)
            .orElseThrow(() -> new RuntimeException("Key not found"));
        
        if (!key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Key belongs to another organization");
        }
        
        key.setKeyName(request.getKeyName());
        key.setDescription(request.getDescription());
        key.setContext(request.getContext());
        
        TranslationKey updated = keyService.update(key);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete translation key")
    public ResponseEntity<?> deleteKey(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationKey key = keyService.getById(id)
            .orElseThrow(() -> new RuntimeException("Key not found"));
        
        if (!key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied: Key belongs to another organization");
        }
        
        keyService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/bulk-delete")
    @Operation(summary = "Delete multiple translation keys")
    public ResponseEntity<?> bulkDeleteKeys(@RequestBody List<Long> ids) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        for (Long id : ids) {
            TranslationKey key = keyService.getById(id).orElse(null);
            if (key != null && !key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
                return ResponseEntity.status(403).body("Access denied: Some keys belong to another organization");
            }
        }
        
        keyService.deleteMultiple(ids);
        return ResponseEntity.ok().build();
    }
    
    private TranslationKeyResponse toResponse(TranslationKey key) {
        Map<String, TranslationValueResponse> translations = new HashMap<>();
        
        if (key.getTranslations() != null) {
            translations = key.getTranslations().stream()
                .collect(Collectors.toMap(
                    t -> t.getLanguage(),
                    t -> TranslationValueResponse.builder()
                        .id(t.getId())
                        .language(t.getLanguage())
                        .value(t.getValue())
                        .status(t.getStatus())
                        .updatedAt(t.getUpdatedAt())
                        .build()
                ));
        }
        
        return TranslationKeyResponse.builder()
            .id(key.getId())
            .appId(key.getApp().getId())
            .keyName(key.getKeyName())
            .description(key.getDescription())
            .context(key.getContext())
            .translations(translations)
            .createdAt(key.getCreatedAt())
            .updatedAt(key.getUpdatedAt())
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
