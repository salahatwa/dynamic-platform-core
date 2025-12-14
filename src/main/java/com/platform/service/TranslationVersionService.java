package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.entity.Translation;
import com.platform.entity.TranslationApp;
import com.platform.entity.TranslationVersion;
import com.platform.entity.User;
import com.platform.repository.TranslationVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TranslationVersionService {
    
    private final TranslationVersionRepository versionRepository;
    private final TranslationService translationService;
    private final ObjectMapper objectMapper;
    
    @Transactional(readOnly = true)
    public List<TranslationVersion> getAllByApp(Long appId) {
        return versionRepository.findByAppIdOrderByVersionDesc(appId);
    }
    
    @Transactional(readOnly = true)
    public Page<TranslationVersion> getAllByApp(Long appId, Pageable pageable) {
        return versionRepository.findByAppId(appId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationVersion> getById(Long id) {
        return versionRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<TranslationVersion> getLatestVersion(Long appId) {
        return versionRepository.findTopByAppIdOrderByVersionDesc(appId);
    }
    
    @Transactional
    public TranslationVersion createVersion(TranslationApp app, String changelog, User createdBy) {
        try {
            // Get current translations
            List<Translation> translations = translationService.getAllByApp(app.getId());
            
            // Create snapshot
            Map<String, Object> snapshot = new HashMap<>();
            Map<String, Map<String, String>> translationsByLanguage = new HashMap<>();
            
            for (Translation t : translations) {
                String lang = t.getLanguage();
                String key = t.getKey().getKeyName();
                String value = t.getValue();
                
                translationsByLanguage.computeIfAbsent(lang, k -> new HashMap<>())
                    .put(key, value);
            }
            
            snapshot.put("translations", translationsByLanguage);
            snapshot.put("totalKeys", translations.stream()
                .map(t -> t.getKey().getId()).distinct().count());
            snapshot.put("languages", translationsByLanguage.keySet());
            
            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            
            // Get next version number
            int nextVersion = versionRepository.findTopByAppIdOrderByVersionDesc(app.getId())
                .map(v -> v.getVersion() + 1)
                .orElse(1);
            
            TranslationVersion version = TranslationVersion.builder()
                .app(app)
                .version(nextVersion)
                .changelog(changelog)
                .snapshot(snapshotJson)
                .createdBy(createdBy)
                .build();
            
            return versionRepository.save(version);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create version: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getVersionSnapshot(Long versionId) {
        try {
            TranslationVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> snapshot = objectMapper.readValue(version.getSnapshot(), Map.class);
            return snapshot;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse version snapshot: " + e.getMessage(), e);
        }
    }
    
    public long countByApp(Long appId) {
        return versionRepository.countByAppId(appId);
    }
}
