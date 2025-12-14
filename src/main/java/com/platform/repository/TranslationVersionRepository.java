package com.platform.repository;

import com.platform.entity.TranslationVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationVersionRepository extends JpaRepository<TranslationVersion, Long> {
    
    // Find by app
    List<TranslationVersion> findByAppIdOrderByVersionDesc(Long appId);
    Page<TranslationVersion> findByAppIdOrderByVersionDesc(Long appId, Pageable pageable);
    Page<TranslationVersion> findByAppId(Long appId, Pageable pageable);
    
    // Find by app and version
    Optional<TranslationVersion> findByAppIdAndVersion(Long appId, Integer version);
    
    // Get latest version
    Optional<TranslationVersion> findTopByAppIdOrderByVersionDesc(Long appId);
    
    @Query("SELECT v FROM TranslationVersion v " +
           "WHERE v.app.id = :appId " +
           "ORDER BY v.version DESC " +
           "LIMIT 1")
    Optional<TranslationVersion> findLatestByAppId(@Param("appId") Long appId);
    
    // Get next version number
    @Query("SELECT COALESCE(MAX(v.version), 0) + 1 FROM TranslationVersion v " +
           "WHERE v.app.id = :appId")
    Integer getNextVersionNumber(@Param("appId") Long appId);
    
    // Count versions by app
    long countByAppId(Long appId);
}
