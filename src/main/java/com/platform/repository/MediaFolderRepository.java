package com.platform.repository;

import com.platform.entity.MediaFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFolderRepository extends JpaRepository<MediaFolder, Long> {

    List<MediaFolder> findByCorporateIdAndParentIsNullOrderByName(Long corporateId);
    
    List<MediaFolder> findByCorporateIdAndParentIdOrderByName(Long corporateId, Long parentId);
    
    Optional<MediaFolder> findByIdAndCorporateId(Long id, Long corporateId);
    
    List<MediaFolder> findByCorporateIdAndAppIdAndParentIsNullOrderByName(Long corporateId, Long appId);
    
    @Query("SELECT f FROM MediaFolder f WHERE f.corporateId = :corporateId AND f.name = :name AND " +
           "(:parentId IS NULL AND f.parent IS NULL OR f.parent.id = :parentId)")
    Optional<MediaFolder> findByCorporateIdAndNameAndParentId(
        @Param("corporateId") Long corporateId, 
        @Param("name") String name, 
        @Param("parentId") Long parentId);
    
    @Query("SELECT COUNT(f) FROM MediaFolder f WHERE f.parent.id = :parentId")
    Integer countChildFolders(@Param("parentId") Long parentId);
    
    @Query("SELECT COUNT(mf) FROM MediaFile mf WHERE mf.folder.id = :folderId")
    Integer countFiles(@Param("folderId") Long folderId);
    
    @Query("SELECT COALESCE(SUM(mf.fileSize), 0) FROM MediaFile mf WHERE mf.folder.id = :folderId")
    Long getTotalSize(@Param("folderId") Long folderId);
    
    boolean existsByCorporateIdAndNameAndParentId(Long corporateId, String name, Long parentId);
    
    boolean existsByCorporateIdAndNameAndParentIsNull(Long corporateId, String name);

    // Additional methods for folder management
    List<MediaFolder> findByParentIdAndCorporateIdAndAppIdOrderByName(Long parentId, Long corporateId, Long appId);
    
    List<MediaFolder> findByParentIsNullAndCorporateIdAndAppIdOrderByName(Long corporateId, Long appId);
    
    List<MediaFolder> findByParentIdAndCorporateIdOrderByName(Long parentId, Long corporateId);
    
    List<MediaFolder> findByParentIsNullAndCorporateIdOrderByName(Long corporateId);
    
    Optional<MediaFolder> findByIdAndCorporateIdAndAppId(Long id, Long corporateId, Long appId);
    
    boolean existsByNameAndParentAndCorporateIdAndAppId(String name, MediaFolder parent, Long corporateId, Long appId);
    
    boolean existsByNameAndParentAndCorporateId(String name, MediaFolder parent, Long corporateId);
    
    boolean existsByNameAndParentAndCorporateIdAndAppIdAndIdNot(String name, MediaFolder parent, Long corporateId, Long appId, Long excludeId);
    
    boolean existsByNameAndParentAndCorporateIdAndIdNot(String name, MediaFolder parent, Long corporateId, Long excludeId);
    
    long countByParentId(Long parentId);
    
    List<MediaFolder> findByParentIdOrderByName(Long parentId);
}