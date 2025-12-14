package com.platform.service;

import com.platform.entity.TranslationKey;
import com.platform.entity.User;
import com.platform.repository.TranslationKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TranslationKeyService {
    
    private final TranslationKeyRepository keyRepository;
    
    @Transactional(readOnly = true)
    public List<TranslationKey> getAllByApp(Long translationAppId) {
        return keyRepository.findByApp_Id(translationAppId);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationKey> getAllByApp(Long translationAppId, Pageable pageable) {
        return keyRepository.findByApp_Id(translationAppId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationKey> searchByApp(Long translationAppId, String search, Pageable pageable) {
        return keyRepository.findByApp_IdAndKeyNameContainingIgnoreCase(translationAppId, search, pageable);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationKey> getById(Long id) {
        return keyRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationKey> getByIdWithTranslations(Long id) {
        return keyRepository.findByIdWithTranslations(id);
    }
    
    @Transactional(readOnly = true)
    public List<TranslationKey> getAllByAppWithTranslations(Long appId) {
        return keyRepository.findByAppIdWithTranslations(appId);
    }
    
    @Transactional(readOnly = true)
    public List<TranslationKey> getKeysWithMissingTranslation(Long appId, String language) {
        return keyRepository.findKeysWithMissingTranslation(appId, language);
    }
    
    @Transactional
    public TranslationKey create(TranslationKey key, User createdBy) {
        key.setCreatedBy(createdBy);
        return keyRepository.save(key);
    }
    
    @Transactional
    public TranslationKey update(TranslationKey key) {
        return keyRepository.save(key);
    }
    
    @Transactional
    public void delete(Long id) {
        keyRepository.deleteById(id);
    }
    
    @Transactional
    public void deleteMultiple(List<Long> ids) {
        keyRepository.deleteAllById(ids);
    }
    
    public boolean existsByAppAndKeyName(Long translationAppId, String keyName) {
        return keyRepository.existsByApp_IdAndKeyName(translationAppId, keyName);
    }
    
    public long countByApp(Long translationAppId) {
        return keyRepository.countByApp_Id(translationAppId);
    }
}
