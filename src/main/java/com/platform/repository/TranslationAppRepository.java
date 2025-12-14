package com.platform.repository;

import com.platform.entity.TranslationApp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationAppRepository extends JpaRepository<TranslationApp, Long> {
    
    // Find by corporate
    List<TranslationApp> findByCorporateId(Long corporateId);
    Page<TranslationApp> findByCorporateId(Long corporateId, Pageable pageable);
    
    // Find by corporate and name
    Optional<TranslationApp> findByCorporateIdAndName(Long corporateId, String name);
    
    // Find by API key
    Optional<TranslationApp> findByApiKey(String apiKey);
    
    // Find active apps by corporate
    List<TranslationApp> findByCorporateIdAndActive(Long corporateId, Boolean active);
    Page<TranslationApp> findByCorporateIdAndActive(Long corporateId, Boolean active, Pageable pageable);
    
    // Search by name
    Page<TranslationApp> findByCorporateIdAndNameContainingIgnoreCase(
        Long corporateId, String name, Pageable pageable);
    
    // Check if name exists for corporate
    boolean existsByCorporateIdAndName(Long corporateId, String name);
    
    // Count apps by corporate
    long countByCorporateId(Long corporateId);
    
    // Find with keys loaded
    @Query("SELECT DISTINCT a FROM TranslationApp a " +
           "LEFT JOIN FETCH a.keys " +
           "WHERE a.id = :id")
    Optional<TranslationApp> findByIdWithKeys(@Param("id") Long id);
    

}
