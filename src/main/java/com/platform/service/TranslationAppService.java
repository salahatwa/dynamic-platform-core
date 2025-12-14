package com.platform.service;

import com.platform.entity.Corporate;
import com.platform.entity.TranslationApp;
import com.platform.repository.TranslationAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TranslationAppService {
    
    private final TranslationAppRepository appRepository;
    
    @Transactional(readOnly = true)
    public List<TranslationApp> getAllByCorporate(Long corporateId) {
        return appRepository.findByCorporateId(corporateId);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationApp> getAllByCorporate(Long corporateId, Pageable pageable) {
        return appRepository.findByCorporateId(corporateId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationApp> searchByCorporate(Long corporateId, String search, Pageable pageable) {
        return appRepository.findByCorporateIdAndNameContainingIgnoreCase(corporateId, search, pageable);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationApp> getById(Long id) {
        return appRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationApp> getByIdWithKeys(Long id) {
        return appRepository.findByIdWithKeys(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationApp> getByApiKey(String apiKey) {
        return appRepository.findByApiKey(apiKey);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationApp> getByNameAndCorporate(String name, Long corporateId) {
        return appRepository.findByCorporateIdAndName(corporateId, name);
    }
    
    @Transactional
    public TranslationApp create(TranslationApp app, Corporate corporate) {
        app.setCorporate(corporate);
        app.setApiKey(generateApiKey());
        return appRepository.save(app);
    }
    
    @Transactional
    public TranslationApp update(TranslationApp app) {
        return appRepository.save(app);
    }
    
    @Transactional
    public void delete(Long id) {
        appRepository.deleteById(id);
    }
    
    @Transactional
    public String regenerateApiKey(Long id) {
        TranslationApp app = appRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("App not found"));
        String newApiKey = generateApiKey();
        app.setApiKey(newApiKey);
        appRepository.save(app);
        return newApiKey;
    }
    
    public boolean belongsToCorporate(Long appId, Long corporateId) {
        return appRepository.findById(appId)
            .map(app -> app.getCorporate().getId().equals(corporateId))
            .orElse(false);
    }
    
    private String generateApiKey() {
        return "ta_" + UUID.randomUUID().toString().replace("-", "");
    }
}
