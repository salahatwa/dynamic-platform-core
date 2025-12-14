package com.platform.repository;

import com.platform.entity.TranslationKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationKeyRepository extends JpaRepository<TranslationKey, Long> {
    
    // Find by translation app (app field is TranslationApp)
    List<TranslationKey> findByApp_Id(Long translationAppId);
    Page<TranslationKey> findByApp_Id(Long translationAppId, Pageable pageable);
    
    // Find by app and key name
    Optional<TranslationKey> findByApp_IdAndKeyName(Long translationAppId, String keyName);
    
    // Search by key name
    Page<TranslationKey> findByApp_IdAndKeyNameContainingIgnoreCase(
        Long translationAppId, String keyName, Pageable pageable);
    
    // Check if key exists
    boolean existsByApp_IdAndKeyName(Long translationAppId, String keyName);
    
    // Count keys by translation app (app field is TranslationApp)
    long countByApp_Id(Long translationAppId);
    
    // Find with translations loaded
    @Query("SELECT DISTINCT k FROM TranslationKey k " +
           "LEFT JOIN FETCH k.translations " +
           "WHERE k.id = :id")
    Optional<TranslationKey> findByIdWithTranslations(@Param("id") Long id);
    
    // Find all with translations for an app
    @Query("SELECT DISTINCT k FROM TranslationKey k " +
           "LEFT JOIN FETCH k.translations t " +
           "WHERE k.app.id = :appId")
    List<TranslationKey> findByAppIdWithTranslations(@Param("appId") Long appId);
    
    // Find keys with missing translations for a language
    @Query("SELECT k FROM TranslationKey k " +
           "WHERE k.app.id = :appId " +
           "AND NOT EXISTS (" +
           "  SELECT t FROM Translation t " +
           "  WHERE t.key = k AND t.language = :language" +
           ")")
    List<TranslationKey> findKeysWithMissingTranslation(
        @Param("appId") Long appId, 
        @Param("language") String language);
    
    // Content API methods
    Optional<TranslationKey> findByKeyNameAndApp_Id(String keyName, Long appId);
    Page<TranslationKey> findByApp_IdAndContextContainingIgnoreCase(Long appId, String context, Pageable pageable);
    Page<TranslationKey> findByApp_IdAndContextContainingIgnoreCaseAndKeyNameContainingIgnoreCase(Long appId, String context, String keyName, Pageable pageable);
    
    @Query("SELECT DISTINCT k.context FROM TranslationKey k WHERE k.app.id = :appId AND k.context IS NOT NULL ORDER BY k.context")
    List<String> findDistinctCategoriesByApp_Id(@Param("appId") Long appId);
}
