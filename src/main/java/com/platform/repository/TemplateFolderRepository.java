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
    
    // Application-scoped queries (new primary methods)
    List<TemplateFolder> findByApplicationIdAndParentIsNullOrderBySortOrder(Long applicationId);
    
    List<TemplateFolder> findByApplicationIdAndParentIdOrderBySortOrder(Long applicationId, Long parentId);
    
    List<TemplateFolder> findByApplicationIdOrderByPath(Long applicationId);
    
    Optional<TemplateFolder> findByIdAndApplicationId(Long id, Long applicationId);
    
    @Query("SELECT f FROM TemplateFolder f WHERE f.application.id = :applicationId AND f.name = :name AND " +
           "((:parentId IS NULL AND f.parent IS NULL) OR f.parent.id = :parentId)")
    Optional<TemplateFolder> findByApplicationIdAndNameAndParentId(
        @Param("applicationId") Long applicationId,
        @Param("name") String name,
        @Param("parentId") Long parentId
    );
    
    boolean existsByIdAndApplicationId(Long id, Long applicationId);
    
    void deleteByIdAndApplicationId(Long id, Long applicationId);
    
    // Hierarchical queries
    @Query("SELECT f FROM TemplateFolder f WHERE f.path LIKE CONCAT(:parentPath, '/%') AND f.application.id = :applicationId")
    List<TemplateFolder> findAllDescendantsByPathAndApplicationId(@Param("parentPath") String parentPath, @Param("applicationId") Long applicationId);
    
    @Query("SELECT f FROM TemplateFolder f WHERE f.level = :level AND f.application.id = :applicationId ORDER BY f.sortOrder")
    List<TemplateFolder> findByLevelAndApplicationIdOrderBySortOrder(@Param("level") Integer level, @Param("applicationId") Long applicationId);
    
    // Tree structure queries with fetch joins
    @Query("SELECT f FROM TemplateFolder f LEFT JOIN FETCH f.children WHERE f.id = :id")
    Optional<TemplateFolder> findByIdWithChildren(@Param("id") Long id);

    @Query("SELECT f FROM TemplateFolder f LEFT JOIN FETCH f.templates WHERE f.id = :id")
    Optional<TemplateFolder> findByIdWithTemplates(@Param("id") Long id);
    
    @Query("SELECT f FROM TemplateFolder f LEFT JOIN FETCH f.corporate LEFT JOIN FETCH f.application WHERE f.id = :id")
    Optional<TemplateFolder> findByIdWithCorporateAndApplication(@Param("id") Long id);
    
    // Statistics queries
    @Query("SELECT COUNT(t) FROM Template t WHERE t.folder.id = :folderId")
    long countTemplatesInFolder(@Param("folderId") Long folderId);
    
    @Query("SELECT COUNT(f) FROM TemplateFolder f WHERE f.parent.id = :folderId")
    long countSubfoldersInFolder(@Param("folderId") Long folderId);
    
    // Legacy corporate-scoped queries (for backward compatibility)
    @Deprecated
    List<TemplateFolder> findByCorporateIdAndParentIsNull(Long corporateId);
    
    @Deprecated
    List<TemplateFolder> findByCorporateIdAndParentId(Long corporateId, Long parentId);
    
    @Deprecated
    List<TemplateFolder> findByCorporateId(Long corporateId);
    
    @Deprecated
    Optional<TemplateFolder> findByIdAndCorporateId(Long id, Long corporateId);
    
    @Deprecated
    @Query("SELECT f FROM TemplateFolder f WHERE f.corporate.id = :corporateId AND f.name = :name AND f.parent.id = :parentId")
    Optional<TemplateFolder> findByCorporateIdAndNameAndParentId(
        @Param("corporateId") Long corporateId,
        @Param("name") String name,
        @Param("parentId") Long parentId
    );
    
    @Deprecated
    @Query("SELECT f FROM TemplateFolder f WHERE f.corporate.id = :corporateId AND f.name = :name AND f.parent IS NULL")
    Optional<TemplateFolder> findByCorporateIdAndNameAndParentIsNull(
        @Param("corporateId") Long corporateId,
        @Param("name") String name
    );
    
    @Deprecated
    boolean existsByIdAndCorporateId(Long id, Long corporateId);
    
    @Deprecated
    void deleteByIdAndCorporateId(Long id, Long corporateId);

    @Deprecated
    List<TemplateFolder> findByParentId(Long parentId);
    
    // Search methods
    List<TemplateFolder> findByApplicationIdAndNameContainingIgnoreCase(Long applicationId, String name);
    
    @Query("SELECT f FROM TemplateFolder f WHERE f.application.id = :applicationId AND " +
           "(f.name LIKE %:searchTerm% OR f.path LIKE %:searchTerm%)")
    List<TemplateFolder> searchByApplicationIdAndTerm(
        @Param("applicationId") Long applicationId,
        @Param("searchTerm") String searchTerm
    );
}
