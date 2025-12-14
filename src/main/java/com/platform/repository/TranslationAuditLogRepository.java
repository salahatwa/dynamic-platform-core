package com.platform.repository;

import com.platform.entity.TranslationAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TranslationAuditLogRepository extends JpaRepository<TranslationAuditLog, Long> {
    
    // Find by app
    Page<TranslationAuditLog> findByAppId(Long appId, Pageable pageable);
    Page<TranslationAuditLog> findByAppIdOrderByTimestampDesc(Long appId, Pageable pageable);
    
    // Find by app and action
    Page<TranslationAuditLog> findByAppIdAndAction(Long appId, String action, Pageable pageable);
    Page<TranslationAuditLog> findByAppIdAndActionOrderByTimestampDesc(
        Long appId, String action, Pageable pageable);
    
    // Find by app and user
    Page<TranslationAuditLog> findByAppIdAndUserId(Long appId, Long userId, Pageable pageable);
    Page<TranslationAuditLog> findByAppIdAndUserIdOrderByTimestampDesc(
        Long appId, Long userId, Pageable pageable);
    
    // Find by app and key
    List<TranslationAuditLog> findByKeyIdOrderByTimestampDesc(Long keyId);
    Page<TranslationAuditLog> findByAppIdAndKeyIdOrderByTimestampDesc(
        Long appId, Long keyId, Pageable pageable);
    
    // Find by app and language
    Page<TranslationAuditLog> findByAppIdAndLanguageOrderByTimestampDesc(
        Long appId, String language, Pageable pageable);
    
    // Find by date range
    Page<TranslationAuditLog> findByAppIdAndTimestampBetween(
        Long appId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT a FROM TranslationAuditLog a " +
           "WHERE a.app.id = :appId " +
           "AND a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<TranslationAuditLog> findByAppIdAndDateRange(
        @Param("appId") Long appId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
    
    // Count by app
    long countByAppId(Long appId);
    
    // Get recent activity
    List<TranslationAuditLog> findTop10ByAppIdOrderByTimestampDesc(Long appId);
}
