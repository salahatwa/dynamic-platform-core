package com.platform.service;

import com.platform.entity.Translation;
import com.platform.entity.User;
import com.platform.enums.TranslationStatus;
import com.platform.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranslationService {
    
    private final TranslationRepository translationRepository;
    
    @Transactional(readOnly = true)
    public List<Translation> getAllByKey(Long keyId) {
        return translationRepository.findByKeyId(keyId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Translation> getByKeyAndLanguage(Long keyId, String language) {
        return translationRepository.findByKeyIdAndLanguage(keyId, language);
    }
    
    @Transactional(readOnly = true)
    public List<Translation> getAllByApp(Long appId) {
        return translationRepository.findByAppId(appId);
    }
    
    @Transactional(readOnly = true)
    public List<Translation> getAllByAppAndLanguage(Long appId, String language) {
        return translationRepository.findByAppIdAndLanguage(appId, language);
    }
    
    @Transactional(readOnly = true)
    public List<Translation> getAllByAppAndStatus(Long appId, TranslationStatus status) {
        return translationRepository.findByAppIdAndStatus(appId, status);
    }
    
    @Transactional(readOnly = true)
    public Map<String, String> getTranslationsAsMap(Long appId, String language) {
        List<Translation> translations = translationRepository.findByAppIdAndLanguageAndStatus(
            appId, language, TranslationStatus.PUBLISHED);
        
        return translations.stream()
            .collect(Collectors.toMap(
                t -> t.getKey().getKeyName(),
                Translation::getValue,
                (v1, v2) -> v1
            ));
    }
    
    @Transactional
    public Translation create(Translation translation, User createdBy) {
        translation.setCreatedBy(createdBy);
        translation.setUpdatedBy(createdBy);
        return translationRepository.save(translation);
    }
    
    @Transactional
    public Translation update(Translation translation, User updatedBy) {
        translation.setUpdatedBy(updatedBy);
        return translationRepository.save(translation);
    }
    
    @Transactional
    public void delete(Long id) {
        translationRepository.deleteById(id);
    }
    
    @Transactional
    public void deleteByKey(Long keyId) {
        translationRepository.deleteByKeyId(keyId);
    }
    
    public boolean existsByKeyAndLanguage(Long keyId, String language) {
        return translationRepository.existsByKeyIdAndLanguage(keyId, language);
    }
    
    public long countByApp(Long appId) {
        return translationRepository.countByAppId(appId);
    }
    
    public long countByAppAndLanguage(Long appId, String language) {
        return translationRepository.countByAppIdAndLanguage(appId, language);
    }
}
