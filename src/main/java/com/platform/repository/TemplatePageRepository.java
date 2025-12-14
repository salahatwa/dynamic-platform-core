package com.platform.repository;

import com.platform.entity.TemplatePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplatePageRepository extends JpaRepository<TemplatePage, Long> {
    
    List<TemplatePage> findByTemplateIdOrderByPageOrderAsc(Long templateId);
    
    @Query("SELECT p FROM TemplatePage p WHERE p.template.id = :templateId AND p.template.corporate.id = :corporateId ORDER BY p.pageOrder ASC")
    List<TemplatePage> findByTemplateIdAndCorporateId(
        @Param("templateId") Long templateId,
        @Param("corporateId") Long corporateId
    );
    
    @Query("SELECT p FROM TemplatePage p WHERE p.id = :id AND p.template.corporate.id = :corporateId")
    Optional<TemplatePage> findByIdAndCorporateId(
        @Param("id") Long id,
        @Param("corporateId") Long corporateId
    );
    
    @Query("SELECT MAX(p.pageOrder) FROM TemplatePage p WHERE p.template.id = :templateId")
    Integer findMaxPageOrderByTemplateId(@Param("templateId") Long templateId);
    
    @Query("SELECT COUNT(p) FROM TemplatePage p WHERE p.template.id = :templateId")
    long countByTemplateId(@Param("templateId") Long templateId);
    
    void deleteByIdAndTemplateCorporateId(Long id, Long corporateId);

    @Modifying
    @Query("UPDATE TemplatePage p SET p.pageOrder = :pageOrder WHERE p.id = :pageId")
    void updatePageOrder(@Param("pageId") Long pageId, @Param("pageOrder") int pageOrder);

    void deleteByTemplateId(Long templateId);
}
