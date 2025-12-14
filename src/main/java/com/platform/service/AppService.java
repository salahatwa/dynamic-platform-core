package com.platform.service;

import com.platform.dto.AppDTO;
import com.platform.dto.AppRequest;
import com.platform.entity.App;
import com.platform.entity.Corporate;
import com.platform.entity.User;
import com.platform.exception.AppLimitExceededException;
import com.platform.exception.AppNotFoundException;
import com.platform.repository.AppRepository;
import com.platform.repository.CorporateRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppService {
    
    private final AppRepository appRepository;
    private final CorporateRepository corporateRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    
    /**
     * Get all apps for a corporate
     */
    @Transactional(readOnly = true)
    public List<AppDTO> getAllApps(Long corporateId) {
        log.info("Getting all apps for corporate: {}", corporateId);
        return appRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId)
            .stream()
            .map(AppDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Get active apps for a corporate
     */
    @Transactional(readOnly = true)
    public List<AppDTO> getActiveApps(Long corporateId) {
        log.info("Getting active apps for corporate: {}", corporateId);
        return appRepository.findActiveByCorporateId(corporateId)
            .stream()
            .map(AppDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Get app by ID
     */
    @Transactional(readOnly = true)
    public AppDTO getAppById(Long appId, Long corporateId) {
        log.info("Getting app {} for corporate {}", appId, corporateId);
        App app = appRepository.findByIdAndCorporateId(appId, corporateId)
            .orElseThrow(() -> new AppNotFoundException(appId));
        return AppDTO.fromEntity(app);
    }
    
    /**
     * Get app by app key
     */
    @Transactional(readOnly = true)
    public AppDTO getAppByKey(String appKey) {
        log.info("Getting app by key: {}", appKey);
        App app = appRepository.findByAppKey(appKey)
            .orElseThrow(() -> new AppNotFoundException(appKey));
        return AppDTO.fromEntity(app);
    }
    
    /**
     * Create new app
     */
    @Transactional
    public AppDTO createApp(AppRequest request, Long corporateId, Long userId) {
        log.info("Creating app '{}' for corporate {}", request.getName(), corporateId);
        
        // Check subscription limits
        if (!subscriptionService.canCreateApp(corporateId)) {
            long currentCount = appRepository.countByCorporateIdAndStatus(corporateId, App.AppStatus.ACTIVE);
            var subscription = subscriptionService.getSubscription(corporateId);
            throw new AppLimitExceededException(
                (int) currentCount + 1, 
                subscription.getMaxApps(), 
                subscription.getTier()
            );
        }
        
        // Check if name already exists
        if (appRepository.existsByCorporateIdAndName(corporateId, request.getName())) {
            throw new RuntimeException("App with name '" + request.getName() + "' already exists");
        }
        
        // Get corporate and user
        Corporate corporate = corporateRepository.findById(corporateId)
            .orElseThrow(() -> new RuntimeException("Corporate not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create app
        App app = new App();
        app.setCorporate(corporate);
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setIconUrl(request.getIconUrl());
        app.setAppKey(generateUniqueAppKey(request.getName()));
        app.setStatus(App.AppStatus.ACTIVE);
        app.setCreatedBy(user);
        
        app = appRepository.save(app);
        log.info("Created app with ID: {} and key: {}", app.getId(), app.getAppKey());
        
        return AppDTO.fromEntity(app);
    }
    
    /**
     * Update app
     */
    @Transactional
    public AppDTO updateApp(Long appId, AppRequest request, Long corporateId) {
        log.info("Updating app {} for corporate {}", appId, corporateId);
        
        App app = appRepository.findByIdAndCorporateId(appId, corporateId)
            .orElseThrow(() -> new AppNotFoundException(appId));
        
        // Check if new name conflicts with existing app
        if (!app.getName().equals(request.getName()) && 
            appRepository.existsByCorporateIdAndName(corporateId, request.getName())) {
            throw new RuntimeException("App with name '" + request.getName() + "' already exists");
        }
        
        app.setName(request.getName());
        app.setDescription(request.getDescription());
        app.setIconUrl(request.getIconUrl());
        
        app = appRepository.save(app);
        log.info("Updated app: {}", appId);
        
        return AppDTO.fromEntity(app);
    }
    
    /**
     * Archive app (soft delete)
     */
    @Transactional
    public void archiveApp(Long appId, Long corporateId) {
        log.info("Archiving app {} for corporate {}", appId, corporateId);
        
        App app = appRepository.findByIdAndCorporateId(appId, corporateId)
            .orElseThrow(() -> new AppNotFoundException(appId));
        
        app.setStatus(App.AppStatus.ARCHIVED);
        appRepository.save(app);
        
        log.info("Archived app: {}", appId);
    }
    
    /**
     * Restore archived app
     */
    @Transactional
    public AppDTO restoreApp(Long appId, Long corporateId) {
        log.info("Restoring app {} for corporate {}", appId, corporateId);
        
        // Check subscription limits
        if (!subscriptionService.canCreateApp(corporateId)) {
            long currentCount = appRepository.countByCorporateIdAndStatus(corporateId, App.AppStatus.ACTIVE);
            var subscription = subscriptionService.getSubscription(corporateId);
            throw new AppLimitExceededException(
                (int) currentCount + 1, 
                subscription.getMaxApps(), 
                subscription.getTier()
            );
        }
        
        App app = appRepository.findByIdAndCorporateId(appId, corporateId)
            .orElseThrow(() -> new AppNotFoundException(appId));
        
        app.setStatus(App.AppStatus.ACTIVE);
        app = appRepository.save(app);
        
        log.info("Restored app: {}", appId);
        return AppDTO.fromEntity(app);
    }
    
    /**
     * Delete app permanently (use with caution)
     */
    @Transactional
    public void deleteApp(Long appId, Long corporateId) {
        log.warn("Permanently deleting app {} for corporate {}", appId, corporateId);
        
        App app = appRepository.findByIdAndCorporateId(appId, corporateId)
            .orElseThrow(() -> new AppNotFoundException(appId));
        
        appRepository.delete(app);
        log.warn("Permanently deleted app: {}", appId);
    }
    
    /**
     * Search apps by name
     */
    @Transactional(readOnly = true)
    public List<AppDTO> searchApps(Long corporateId, String search) {
        log.info("Searching apps for corporate {} with query: {}", corporateId, search);
        return appRepository.searchByName(corporateId, search)
            .stream()
            .map(AppDTO::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Count apps for corporate
     */
    @Transactional(readOnly = true)
    public long countApps(Long corporateId) {
        return appRepository.countByCorporateId(corporateId);
    }
    
    /**
     * Count active apps for corporate
     */
    @Transactional(readOnly = true)
    public long countActiveApps(Long corporateId) {
        return appRepository.countByCorporateIdAndStatus(corporateId, App.AppStatus.ACTIVE);
    }
    
    /**
     * Generate unique app key
     */
    private String generateUniqueAppKey(String appName) {
        String baseKey = "app_" + appName.toLowerCase()
            .replaceAll("[^a-z0-9]", "_")
            .replaceAll("_{2,}", "_")
            .replaceAll("^_|_$", "");
        
        String appKey = baseKey;
        int counter = 1;
        
        while (appRepository.existsByAppKey(appKey)) {
            appKey = baseKey + "_" + counter++;
        }
        
        // Add random suffix for extra uniqueness
        appKey = appKey + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        return appKey;
    }
    
    /**
     * Validate app belongs to corporate
     */
    @Transactional(readOnly = true)
    public boolean validateAppBelongsToCorporate(Long appId, Long corporateId) {
        return appRepository.findByIdAndCorporateId(appId, corporateId).isPresent();
    }
}
