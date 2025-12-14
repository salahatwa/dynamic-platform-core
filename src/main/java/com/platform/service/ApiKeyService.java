package com.platform.service;

import com.platform.entity.ApiKey;
import com.platform.entity.App;
import com.platform.entity.User;
import com.platform.repository.ApiKeyRepository;
import com.platform.repository.AppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final AppRepository appRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    public ApiKey generateApiKey(User user, Long appId, String name, String description, Integer expiryDays) {
        String keyValue = generateSecureKey();
        
        // Get the app
        App app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App not found"));
        
        ApiKey apiKey = ApiKey.builder()
                .keyValue(keyValue)
                .name(name)
                .description(description)
                .user(user)
                .corporate(user.getCorporate())
                .app(app)
                .active(true)
                .build();
        
        if (expiryDays != null && expiryDays > 0) {
            apiKey.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));
        }
        
        return apiKeyRepository.save(apiKey);
    }
    
    // Backward compatibility method
    @Deprecated
    public ApiKey generateApiKey(User user, String name, String description, Integer expiryDays) {
        throw new RuntimeException("API Keys must be associated with an app. Use generateApiKey(user, appId, name, description, expiryDays) instead.");
    }
    
    public List<ApiKey> getUserApiKeys(Long userId) {
        return apiKeyRepository.findByUserId(userId);
    }
    
    public List<ApiKey> getCorporateApiKeys(Long corporateId) {
        return apiKeyRepository.findByCorporateId(corporateId);
    }
    
    // App-centric methods
    public List<ApiKey> getAppApiKeys(Long appId) {
        return apiKeyRepository.findByApp_Id(appId);
    }
    
    public List<ApiKey> getAppApiKeys(Long appId, Boolean active) {
        return apiKeyRepository.findByApp_IdAndActive(appId, active);
    }
    
    public List<ApiKey> getAppApiKeys(Long appId, Long corporateId) {
        return apiKeyRepository.findByApp_IdAndCorporateId(appId, corporateId);
    }
    
    public List<ApiKey> getAppApiKeys(Long appId, Long corporateId, Boolean active) {
        return apiKeyRepository.findByApp_IdAndCorporateIdAndActive(appId, corporateId, active);
    }
    
    public void revokeApiKey(Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API Key not found"));
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }
    
    public void deleteApiKey(Long apiKeyId) {
        apiKeyRepository.deleteById(apiKeyId);
    }
    
    public boolean validateApiKey(String keyValue) {
        return apiKeyRepository.findByKeyValue(keyValue)
                .map(apiKey -> {
                    if (!apiKey.getActive()) return false;
                    if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    apiKey.setLastUsedAt(LocalDateTime.now());
                    apiKeyRepository.save(apiKey);
                    return true;
                })
                .orElse(false);
    }
    
    private String generateSecureKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "tms_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
