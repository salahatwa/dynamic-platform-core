package com.platform.repository;

import com.platform.entity.TemplateFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateFolderRepository extends JpaRepository<TemplateFolder, Long> {
    
    List<TemplateFolder> findByCorporateIdAndParentIsNull(Long corporateId);
    
    List<TemplateFolder> findByCorporateIdAndParentId(Long corporateId, Long parentId);
    
    List<TemplateFolder> findByCorporateId(Long corporateId);
    
    Optional<TemplateFolder> findByIdAndCorporateId(Long id, Long corporateId);
    
    @Query("SELECT f FROM TemplateFolder f WHERE f.corporate.id = :corporateId AND f.name = :name AND f.parent.id = :parentId")
    Optional<TemplateFolder> findByCorporateIdAndNameAndParentId(
        @Param("corporateId") Long corporateId,
        @Param("name") String name,
        @Param("parentId") Long parentId
    );
    
    @Query("SELECT f FROM TemplateFolder f WHERE f.corporate.id = :corporateId AND f.name = :name AND f.parent IS NULL")
    Optional<TemplateFolder> findByCorporateIdAndNameAndParentIsNull(
        @Param("corporateId") Long corporateId,
        @Param("name") String name
    );
    
    boolean existsByIdAndCorporateId(Long id, Long corporateId);
    
    void deleteByIdAndCorporateId(Long id, Long corporateId);

    List<TemplateFolder> findByParentId(Long parentId);

    @Query("SELECT f FROM TemplateFolder f LEFT JOIN FETCH f.children WHERE f.id = :id")
    Optional<TemplateFolder> findByIdWithChildren(@Param("id") Long id);

    @Query("SELECT f FROM TemplateFolder f LEFT JOIN FETCH f.templates WHERE f.id = :id")
    Optional<TemplateFolder> findByIdWithTemplates(@Param("id") Long id);

    @Query("SELECT COUNT(t) FROM Template t WHERE t.folder.id = :folderId")
    long countTemplatesInFolder(@Param("folderId") Long folderId);
}
