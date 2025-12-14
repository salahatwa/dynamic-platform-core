package com.platform.repository;

import com.platform.entity.Translation;
import com.platform.enums.TranslationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {
    
    // Find by key
    List<Translation> findByKeyId(Long keyId);
    
    // Find by key and language
    Optional<Translation> findByKeyIdAndLanguage(Long keyId, String language);
    
    // Find by language for an app
    @Query("SELECT t FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId AND t.language = :language")
    List<Translation> findByAppIdAndLanguage(
        @Param("appId") Long appId, 
        @Param("language") String language);
    
    // Find all translations for an app
    @Query("SELECT t FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId")
    List<Translation> findByAppId(@Param("appId") Long appId);
    
    // Find by status
    @Query("SELECT t FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId AND t.status = :status")
    List<Translation> findByAppIdAndStatus(
        @Param("appId") Long appId, 
        @Param("status") TranslationStatus status);
    
    // Find by language and status
    @Query("SELECT t FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId " +
           "AND t.language = :language " +
           "AND t.status = :status")
    List<Translation> findByAppIdAndLanguageAndStatus(
        @Param("appId") Long appId,
        @Param("language") String language,
        @Param("status") TranslationStatus status);
    
    // Count translations by app
    @Query("SELECT COUNT(t) FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId")
    long countByAppId(@Param("appId") Long appId);
    
    // Count translations by translation app
    @Query("SELECT COUNT(t) FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :translationAppId")
    long countByTranslationAppId(@Param("translationAppId") Long translationAppId);
    
    // Count distinct languages by translation app
    @Query("SELECT COUNT(DISTINCT t.language) FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :translationAppId")
    long countDistinctLanguagesByTranslationAppId(@Param("translationAppId") Long translationAppId);
    
    // Count translations by app and language
    @Query("SELECT COUNT(t) FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId AND t.language = :language")
    long countByAppIdAndLanguage(
        @Param("appId") Long appId, 
        @Param("language") String language);
    
    // Delete by key
    void deleteByKeyId(Long keyId);
    
    // Check if translation exists
    boolean existsByKeyIdAndLanguage(Long keyId, String language);
    
    // Content API methods
    @Query("SELECT DISTINCT t.language FROM Translation t " +
           "JOIN t.key k " +
           "WHERE k.app.id = :appId ORDER BY t.language")
    List<String> findDistinctLanguagesByApp_Id(@Param("appId") Long appId);
}
