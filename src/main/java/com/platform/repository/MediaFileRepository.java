package com.platform.repository;

import com.platform.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByIdAndCorporateId(Long id, Long corporateId);
    
    Optional<MediaFile> findByIdAndCorporateIdAndAppId(Long id, Long corporateId, Long appId);
    
    List<MediaFile> findByFolderIdAndCorporateIdOrderByFilename(Long folderId, Long corporateId);
    
    List<MediaFile> findByFolderIdAndCorporateIdAndAppIdOrderByFilename(Long folderId, Long corporateId, Long appId);
    
    Page<MediaFile> findByCorporateIdOrderByCreatedAtDesc(Long corporateId, Pageable pageable);
    
    Page<MediaFile> findByCorporateIdAndAppIdOrderByCreatedAtDesc(Long corporateId, Long appId, Pageable pageable);
    
    Page<MediaFile> findByFolderIdAndCorporateIdOrderByFilename(Long folderId, Long corporateId, Pageable pageable);
    
    Page<MediaFile> findByFolderIdAndCorporateIdAndAppIdOrderByFilename(Long folderId, Long corporateId, Long appId, Pageable pageable);
    
    // Simple queries without search to avoid PostgreSQL casting issues
    @Query("SELECT mf FROM MediaFile mf WHERE mf.corporateId = :corporateId " +
           "AND (:folderId IS NULL OR mf.folder.id = :folderId) " +
           "AND (:mimeType IS NULL OR mf.mimeType LIKE :mimeType) " +
           "ORDER BY mf.createdAt DESC, mf.updatedAt DESC")
    Page<MediaFile> findByCorporateIdWithBasicFilters(
        @Param("corporateId") Long corporateId,
        @Param("folderId") Long folderId,
        @Param("mimeType") String mimeType,
        Pageable pageable);

    @Query("SELECT mf FROM MediaFile mf WHERE mf.corporateId = :corporateId " +
           "AND mf.appId = :appId " +
           "AND (:folderId IS NULL OR mf.folder.id = :folderId) " +
           "AND (:mimeType IS NULL OR mf.mimeType LIKE :mimeType) " +
           "ORDER BY mf.createdAt DESC, mf.updatedAt DESC")
    Page<MediaFile> findByCorporateIdAndAppIdWithBasicFilters(
        @Param("corporateId") Long corporateId,
        @Param("appId") Long appId,
        @Param("folderId") Long folderId,
        @Param("mimeType") String mimeType,
        Pageable pageable);

    // Advanced queries with search (using native SQL to handle PostgreSQL properly)
    @Query(value = "SELECT * FROM media_files mf WHERE mf.corporate_id = :corporateId " +
           "AND (:folderId IS NULL OR mf.folder_id = :folderId) " +
           "AND (:mimeType IS NULL OR mf.mime_type LIKE :mimeType) " +
           "AND (:search IS NULL OR LOWER(mf.filename::text) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(mf.original_filename::text) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY mf.created_at DESC, mf.updated_at DESC",
           countQuery = "SELECT COUNT(*) FROM media_files mf WHERE mf.corporate_id = :corporateId " +
           "AND (:folderId IS NULL OR mf.folder_id = :folderId) " +
           "AND (:mimeType IS NULL OR mf.mime_type LIKE :mimeType) " +
           "AND (:search IS NULL OR LOWER(mf.filename::text) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(mf.original_filename::text) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<MediaFile> findByCorporateIdWithFilters(
        @Param("corporateId") Long corporateId,
        @Param("folderId") Long folderId,
        @Param("mimeType") String mimeType,
        @Param("search") String search,
        Pageable pageable);

    @Query(value = "SELECT * FROM media_files mf WHERE mf.corporate_id = :corporateId " +
           "AND mf.app_id = :appId " +
           "AND (:folderId IS NULL OR mf.folder_id = :folderId) " +
           "AND (:mimeType IS NULL OR mf.mime_type LIKE :mimeType) " +
           "AND (:search IS NULL OR LOWER(mf.filename::text) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(mf.original_filename::text) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY mf.created_at DESC, mf.updated_at DESC",
           countQuery = "SELECT COUNT(*) FROM media_files mf WHERE mf.corporate_id = :corporateId " +
           "AND mf.app_id = :appId " +
           "AND (:folderId IS NULL OR mf.folder_id = :folderId) " +
           "AND (:mimeType IS NULL OR mf.mime_type LIKE :mimeType) " +
           "AND (:search IS NULL OR LOWER(mf.filename::text) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(mf.original_filename::text) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<MediaFile> findByCorporateIdAndAppIdWithFilters(
        @Param("corporateId") Long corporateId,
        @Param("appId") Long appId,
        @Param("folderId") Long folderId,
        @Param("mimeType") String mimeType,
        @Param("search") String search,
        Pageable pageable);
    
    List<MediaFile> findByFileHashAndCorporateId(String fileHash, Long corporateId);
    
    @Query("SELECT COUNT(mf) FROM MediaFile mf WHERE mf.corporateId = :corporateId")
    Long countByCorporateId(@Param("corporateId") Long corporateId);
    
    @Query("SELECT COALESCE(SUM(mf.fileSize), 0) FROM MediaFile mf WHERE mf.corporateId = :corporateId")
    Long getTotalSizeByCorporateId(@Param("corporateId") Long corporateId);
    
    @Query("SELECT mf FROM MediaFile mf WHERE mf.status = :status AND mf.expiresAt < CURRENT_TIMESTAMP")
    List<MediaFile> findExpiredFiles(@Param("status") MediaFile.MediaStatus status);
    
    List<MediaFile> findByProviderTypeAndProviderKey(MediaFile.MediaProviderType providerType, String providerKey);
    
    Optional<MediaFile> findByProviderKey(String providerKey);
    
    // Additional methods for folder support
    long countByFolderId(Long folderId);
    
    List<MediaFile> findByFolderIdOrderByFilename(Long folderId);
}