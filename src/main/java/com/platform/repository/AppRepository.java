package com.platform.repository;

import com.platform.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppRepository extends JpaRepository<App, Long> {
    
    /**
     * Find all apps for a corporate with specific status
     */
    List<App> findByCorporateIdAndStatus(Long corporateId, App.AppStatus status);
    
    /**
     * Find all apps for a corporate (any status)
     */
    List<App> findByCorporateIdOrderByCreatedAtDesc(Long corporateId);
    
    /**
     * Find app by unique app key
     */
    Optional<App> findByAppKey(String appKey);
    
    /**
     * Find app by ID and corporate ID (for security)
     */
    Optional<App> findByIdAndCorporateId(Long id, Long corporateId);
    
    /**
     * Check if app name exists for corporate
     */
    boolean existsByCorporateIdAndName(Long corporateId, String name);
    
    /**
     * Check if app key exists
     */
    boolean existsByAppKey(String appKey);
    
    /**
     * Count active apps for a corporate
     */
    long countByCorporateIdAndStatus(Long corporateId, App.AppStatus status);
    
    /**
     * Count all apps for a corporate
     */
    long countByCorporateId(Long corporateId);
    
    /**
     * Find active apps for a corporate
     */
    @Query("SELECT a FROM App a WHERE a.corporate.id = :corporateId AND a.status = 'ACTIVE' ORDER BY a.createdAt DESC")
    List<App> findActiveByCorporateId(@Param("corporateId") Long corporateId);
    
    /**
     * Search apps by name
     */
    @Query("SELECT a FROM App a WHERE a.corporate.id = :corporateId AND LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY a.createdAt DESC")
    List<App> searchByName(@Param("corporateId") Long corporateId, @Param("search") String search);
    
    /**
     * Find app by name and corporate ID
     */
    Optional<App> findByCorporateIdAndName(Long corporateId, String name);
}
