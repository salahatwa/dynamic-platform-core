package com.platform.repository;

import com.platform.entity.Template;
import com.platform.enums.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    // App-specific queries
    List<Template> findByApp_Id(Long appId);
    List<Template> findByApp_IdAndType(Long appId, TemplateType type);
    org.springframework.data.domain.Page<Template> findByApp_Id(Long appId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Template> findByApp_IdAndNameContainingIgnoreCase(Long appId, String name, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Template> findByApp_IdAndType(Long appId, TemplateType type, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Template> findByApp_IdAndTypeAndNameContainingIgnoreCase(Long appId, TemplateType type, String name, org.springframework.data.domain.Pageable pageable);
    
    // Content API methods
    java.util.Optional<Template> findByIdAndApp_Id(Long id, Long appId);
    
    // Corporate-filtered queries with pagination
    List<Template> findByCorporateId(Long corporateId);
    List<Template> findByCorporateIdAndType(Long corporateId, TemplateType type);
    org.springframework.data.domain.Page<Template> findByCorporateId(Long corporateId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Template> findByCorporateIdAndNameContainingIgnoreCase(Long corporateId, String name, org.springframework.data.domain.Pageable pageable);
    
    // App-based queries with corporate filtering (for admin controllers)
    org.springframework.data.domain.Page<Template> findByApp_NameAndCorporateId(String appName, Long corporateId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Template> findByApp_NameAndCorporateIdAndNameContainingIgnoreCase(String appName, Long corporateId, String name, org.springframework.data.domain.Pageable pageable);
    
    // Dashboard count methods
    long countByApp_Id(Long appId);
    long countByApp_IdAndActiveTrue(Long appId);
    
    long countByCorporateId(Long corporateId);
    long countByCorporateIdAndActiveTrue(Long corporateId);
}
