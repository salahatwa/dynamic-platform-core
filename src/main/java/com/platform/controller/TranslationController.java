package com.platform.controller;

import com.platform.dto.BulkTranslationRequest;
import com.platform.dto.TranslationRequest;
import com.platform.entity.Translation;
import com.platform.entity.TranslationKey;
import com.platform.entity.User;
import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.UserRepository;
import com.platform.security.RequirePermission;
import com.platform.security.UserPrincipal;
import com.platform.service.TranslationKeyService;
import com.platform.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/translations")
@RequiredArgsConstructor
@Tag(name = "Translations", description = "Translation value management")
public class TranslationController {
    
    private final TranslationService translationService;
    private final TranslationKeyService keyService;
    private final UserRepository userRepository;
    
    @GetMapping("/key/{keyId}")
    @RequirePermission(resource = PermissionResource.TRANSLATIONS, action = PermissionAction.READ)
    @Operation(summary = "Get all translations for a key")
    public ResponseEntity<?> getTranslationsByKey(@PathVariable Long keyId) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationKey key = keyService.getById(keyId)
            .orElseThrow(() -> new RuntimeException("Key not found"));
        
        if (!key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        List<Translation> translations = translationService.getAllByKey(keyId);
        return ResponseEntity.ok(translations);
    }
    
    @PostMapping
    @RequirePermission(resource = PermissionResource.TRANSLATIONS, action = PermissionAction.CREATE)
    @Operation(summary = "Create or update translation")
    public ResponseEntity<?> createOrUpdateTranslation(@Valid @RequestBody TranslationRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        TranslationKey key = keyService.getById(request.getKeyId())
            .orElseThrow(() -> new RuntimeException("Key not found"));
        
        if (!key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        Translation translation = translationService.getByKeyAndLanguage(request.getKeyId(), request.getLanguage())
            .orElse(Translation.builder()
                .key(key)
                .language(request.getLanguage())
                .build());
        
        translation.setValue(request.getValue());
        translation.setStatus(request.getStatus());
        
        Translation saved;
        if (translation.getId() == null) {
            saved = translationService.create(translation, currentUser);
        } else {
            saved = translationService.update(translation, currentUser);
        }
        
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/{id}")
    @RequirePermission(resource = PermissionResource.TRANSLATIONS, action = PermissionAction.UPDATE)
    @Operation(summary = "Update translation")
    public ResponseEntity<?> updateTranslation(@PathVariable Long id, @Valid @RequestBody TranslationRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        Translation translation = translationService.getAllByKey(request.getKeyId()).stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Translation not found"));
        
        if (!translation.getKey().getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        
        translation.setValue(request.getValue());
        translation.setStatus(request.getStatus());
        
        Translation updated = translationService.update(translation, currentUser);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    @RequirePermission(resource = PermissionResource.TRANSLATIONS, action = PermissionAction.DELETE)
    @Operation(summary = "Delete translation")
    public ResponseEntity<?> deleteTranslation(@PathVariable Long id) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        translationService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/bulk")
    @RequirePermission(resource = PermissionResource.TRANSLATIONS, action = PermissionAction.CREATE)
    @Operation(summary = "Bulk create/update translations")
    public ResponseEntity<?> bulkCreateOrUpdate(@RequestBody BulkTranslationRequest request) {
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        List<Translation> saved = new ArrayList<>();
        
        if (request.getItems() != null) {
            for (TranslationRequest item : request.getItems()) {
                TranslationKey key = keyService.getById(item.getKeyId())
                    .orElseThrow(() -> new RuntimeException("Key not found: " + item.getKeyId()));
                
                if (!key.getApp().getCorporate().getId().equals(currentUser.getCorporate().getId())) {
                    return ResponseEntity.status(403).body("Access denied");
                }
                
                Translation translation = translationService.getByKeyAndLanguage(item.getKeyId(), item.getLanguage())
                    .orElse(Translation.builder()
                        .key(key)
                        .language(item.getLanguage())
                        .build());
                
                translation.setValue(item.getValue());
                translation.setStatus(item.getStatus());
                
                if (translation.getId() == null) {
                    saved.add(translationService.create(translation, currentUser));
                } else {
                    saved.add(translationService.update(translation, currentUser));
                }
            }
        }
        
        return ResponseEntity.ok(saved);
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
