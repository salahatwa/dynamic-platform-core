package com.platform.repository;

import com.platform.entity.TemplateAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateAttributeRepository extends JpaRepository<TemplateAttribute, Long> {
    
    List<TemplateAttribute> findByTemplateId(Long templateId);
    
    @Query("SELECT a FROM TemplateAttribute a WHERE a.template.id = :templateId AND a.template.corporate.id = :corporateId")
    List<TemplateAttribute> findByTemplateIdAndCorporateId(
        @Param("templateId") Long templateId,
        @Param("corporateId") Long corporateId
    );
    
    @Query("SELECT a FROM TemplateAttribute a WHERE a.id = :id AND a.template.corporate.id = :corporateId")
    Optional<TemplateAttribute> findByIdAndCorporateId(
        @Param("id") Long id,
        @Param("corporateId") Long corporateId
    );
    
    @Query("SELECT a FROM TemplateAttribute a WHERE a.template.id = :templateId AND a.attributeKey = :key")
    Optional<TemplateAttribute> findByTemplateIdAndAttributeKey(
        @Param("templateId") Long templateId,
        @Param("key") String key
    );
    
    boolean existsByTemplateIdAndAttributeKey(Long templateId, String attributeKey);
    
    void deleteByIdAndTemplateCorporateId(Long id, Long corporateId);
    
    void deleteByTemplateIdAndAttributeKey(Long templateId, String attributeKey);

	void deleteByTemplateId(Long templateId);

	boolean existsByTemplateIdAndAttributeKeyAndIdNot(Long templateId, String key, Long excludeId);
}
