package com.platform.controller;

import com.platform.dto.ApiKeyRequest;
import com.platform.dto.ApiKeyResponse;
import com.platform.entity.ApiKey;
import com.platform.entity.User;
import com.platform.repository.UserRepository;
import com.platform.security.UserPrincipal;
import com.platform.service.ApiKeyService;
import com.platform.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "API key management endpoints")
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    
    @PostMapping
    @Operation(summary = "Generate new API key")
    public ResponseEntity<?> generateApiKey(
            @RequestBody ApiKeyRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        User user = getCurrentUserWithCorporate();
        if (user == null || user.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        if (request.getAppId() == null) {
            return ResponseEntity.badRequest().body("App ID is required for API key generation");
        }
        
        ApiKey apiKey = apiKeyService.generateApiKey(
                user,
                request.getAppId(),
                request.getName(),
                request.getDescription(),
                request.getExpiryDays()
        );
        
        // Log audit
        auditLogService.log(
            "CREATE",
            "API_KEY",
            apiKey.getId(),
            apiKey.getName(),
            user,
            "Generated new API key for app: " + apiKey.getApp().getName(),
            httpRequest.getRemoteAddr()
        );
        
        return ResponseEntity.ok(mapToResponse(apiKey));
    }
    
    @GetMapping
    @Operation(summary = "Get API keys for app or corporate")
    public ResponseEntity<?> getApiKeys(
            @RequestParam(required = false) Long appId,
            Authentication authentication) {
        
        User user = getCurrentUserWithCorporate();
        if (user == null || user.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        List<ApiKeyResponse> apiKeys;
        
        if (appId != null) {
            // Get API keys for specific app
            apiKeys = apiKeyService.getAppApiKeys(appId, user.getCorporate().getId())
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } else {
            // Get all API keys for corporate (backward compatibility)
            apiKeys = apiKeyService.getCorporateApiKeys(user.getCorporate().getId())
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(apiKeys);
    }
    
    @PutMapping("/{id}/revoke")
    @Operation(summary = "Revoke API key")
    public ResponseEntity<?> revokeApiKey(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        ApiKey apiKey = apiKeyService.getCorporateApiKeys(currentUser.getCorporate().getId()).stream()
                .filter(k -> k.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("API key not found or access denied"));
        
        apiKeyService.revokeApiKey(id);
        
        // Log audit
        auditLogService.log(
            "REVOKE",
            "API_KEY",
            id,
            apiKey.getName(),
            currentUser,
            "Revoked API key",
            httpRequest.getRemoteAddr()
        );
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete API key")
    public ResponseEntity<?> deleteApiKey(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        User currentUser = getCurrentUserWithCorporate();
        if (currentUser == null || currentUser.getCorporate() == null) {
            return ResponseEntity.badRequest().body("User not associated with any organization");
        }
        
        ApiKey apiKey = apiKeyService.getCorporateApiKeys(currentUser.getCorporate().getId()).stream()
                .filter(k -> k.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("API key not found or access denied"));
        
        String keyName = apiKey.getName();
        apiKeyService.deleteApiKey(id);
        
        // Log audit
        auditLogService.log(
            "DELETE",
            "API_KEY",
            id,
            keyName,
            currentUser,
            "Deleted API key",
            httpRequest.getRemoteAddr()
        );
        
        return ResponseEntity.ok().build();
    }
    
    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .keyValue(apiKey.getKeyValue())
                .name(apiKey.getName())
                .description(apiKey.getDescription())
                .appId(apiKey.getApp() != null ? apiKey.getApp().getId() : null)
                .appName(apiKey.getApp() != null ? apiKey.getApp().getName() : null)
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .active(apiKey.getActive())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userRepository.findByEmail(userPrincipal.getEmail()).orElse(null);
        }
        return null;
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
