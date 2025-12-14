package com.platform.repository;

import com.platform.entity.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ErrorCodeRepository extends JpaRepository<ErrorCode, Long> {
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.corporateId = :corporateId OR e.corporateId IS NULL ORDER BY e.errorCode")
    List<ErrorCode> findAllByCorporateId(@Param("corporateId") Long corporateId);
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.errorCode = :errorCode AND (e.corporateId = :corporateId OR e.corporateId IS NULL)")
    Optional<ErrorCode> findByErrorCodeAndCorporateId(@Param("errorCode") String errorCode, @Param("corporateId") Long corporateId);
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.app.id = :appId AND (e.corporateId = :corporateId OR e.corporateId IS NULL) ORDER BY e.errorCode")
    List<ErrorCode> findByApp_IdAndCorporateId(@Param("appId") Long appId, @Param("corporateId") Long corporateId);
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.app.id = :appId AND e.isPublic = true AND e.status = 'ACTIVE' AND (e.corporateId = :corporateId OR e.corporateId IS NULL) ORDER BY e.errorCode")
    List<ErrorCode> findPublicByApp_IdAndCorporateId(@Param("appId") Long appId, @Param("corporateId") Long corporateId);
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.category.id = :categoryId AND (e.corporateId = :corporateId OR e.corporateId IS NULL) ORDER BY e.errorCode")
    List<ErrorCode> findByCategoryIdAndCorporateId(@Param("categoryId") Long categoryId, @Param("corporateId") Long corporateId);
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.severity = :severity AND (e.corporateId = :corporateId OR e.corporateId IS NULL) ORDER BY e.errorCode")
    List<ErrorCode> findBySeverityAndCorporateId(@Param("severity") ErrorCode.ErrorSeverity severity, @Param("corporateId") Long corporateId);
    
    @Query("SELECT e FROM ErrorCode e LEFT JOIN FETCH e.category LEFT JOIN FETCH e.translations WHERE e.status = :status AND (e.corporateId = :corporateId OR e.corporateId IS NULL) ORDER BY e.errorCode")
    List<ErrorCode> findByStatusAndCorporateId(@Param("status") ErrorCode.ErrorStatus status, @Param("corporateId") Long corporateId);
    
    @Query("SELECT DISTINCT a.name FROM ErrorCode e JOIN e.app a WHERE e.corporateId = :corporateId OR e.corporateId IS NULL ORDER BY a.name")
    List<String> findDistinctAppNames(@Param("corporateId") Long corporateId);
    
    @Query("SELECT DISTINCT e.moduleName FROM ErrorCode e WHERE e.app.name = :appName AND (e.corporateId = :corporateId OR e.corporateId IS NULL) ORDER BY e.moduleName")
    List<String> findDistinctModulesByAppName(@Param("appName") String appName, @Param("corporateId") Long corporateId);
    
    boolean existsByErrorCodeAndCorporateId(String errorCode, Long corporateId);
    
    Optional<ErrorCode> findByIdAndCorporateId(Long id, Long corporateId);
    
    @Query("SELECT DISTINCT e.app.name FROM ErrorCode e WHERE e.corporateId = :corporateId OR e.corporateId IS NULL ORDER BY e.app.name")
    List<String> findDistinctAppNamesByCorporateId(@Param("corporateId") Long corporateId);
    
    @Query(value = "SELECT e.* FROM error_code e LEFT JOIN error_code_category c ON c.id = e.category_id LEFT JOIN apps a ON a.id = e.app_id WHERE " +
           "(e.corporate_id = :corporateId OR e.corporate_id IS NULL) " +
           "AND (:appName IS NULL OR a.name = :appName) " +
           "AND (:categoryId IS NULL OR e.category_id = :categoryId) " +
           "AND (:severity IS NULL OR e.severity = CAST(:severity AS VARCHAR)) " +
           "AND (:status IS NULL OR e.status = CAST(:status AS VARCHAR)) " +
           "AND (:search IS NULL OR LOWER(e.error_code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.default_message) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(e.id) FROM error_code e LEFT JOIN apps a ON a.id = e.app_id WHERE " +
           "(e.corporate_id = :corporateId OR e.corporate_id IS NULL) " +
           "AND (:appName IS NULL OR a.name = :appName) " +
           "AND (:categoryId IS NULL OR e.category_id = :categoryId) " +
           "AND (:severity IS NULL OR e.severity = CAST(:severity AS VARCHAR)) " +
           "AND (:status IS NULL OR e.status = CAST(:status AS VARCHAR)) " +
           "AND (:search IS NULL OR LOWER(e.error_code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.default_message) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<ErrorCode> findAllWithFilters(
        @Param("corporateId") Long corporateId,
        @Param("appName") String appName,
        @Param("categoryId") Long categoryId,
        @Param("severity") String severity,
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );
    
    // Dashboard count methods
    long countByApp_Id(Long appId);
    long countByApp_IdAndStatus(Long appId, ErrorCode.ErrorStatus status);
    
    long countByCorporateId(Long corporateId);
    
    // Content API methods
    @Query("SELECT e FROM ErrorCode e WHERE e.errorCode = :errorCode AND e.app.id = :appId")
    Optional<ErrorCode> findByErrorCodeAndAppId(@Param("errorCode") String errorCode, @Param("appId") Long appId);
    
    @Query(value = "SELECT e.* FROM error_code e LEFT JOIN error_code_category c ON c.id = e.category_id WHERE " +
           "e.app_id = :appId " +
           "AND (:categoryId IS NULL OR e.category_id = :categoryId) " +
           "AND (:severity IS NULL OR e.severity = CAST(:severity AS VARCHAR)) " +
           "AND (:status IS NULL OR e.status = CAST(:status AS VARCHAR)) " +
           "AND (:module IS NULL OR e.module_name = :module) " +
           "AND (:search IS NULL OR LOWER(e.error_code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.default_message) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY e.error_code",
           countQuery = "SELECT COUNT(e.id) FROM error_code e WHERE " +
           "e.app_id = :appId " +
           "AND (:categoryId IS NULL OR e.category_id = :categoryId) " +
           "AND (:severity IS NULL OR e.severity = CAST(:severity AS VARCHAR)) " +
           "AND (:status IS NULL OR e.status = CAST(:status AS VARCHAR)) " +
           "AND (:module IS NULL OR e.module_name = :module) " +
           "AND (:search IS NULL OR LOWER(e.error_code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(e.default_message) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<ErrorCode> findAllWithFiltersForContent(
        @Param("appId") Long appId,
        @Param("categoryId") Long categoryId,
        @Param("severity") String severity,
        @Param("status") String status,
        @Param("module") String module,
        @Param("search") String search,
        Pageable pageable
    );
    
    @Query(value = "SELECT DISTINCT c.id, c.category_name, c.description FROM error_code e " +
           "JOIN error_code_category c ON c.id = e.category_id " +
           "WHERE e.app_id = :appId ORDER BY c.category_name", nativeQuery = true)
    List<Map<String, Object>> findCategoriesByAppId(@Param("appId") Long appId);
    
    @Query("SELECT DISTINCT e.moduleName FROM ErrorCode e WHERE e.app.id = :appId AND e.moduleName IS NOT NULL ORDER BY e.moduleName")
    List<String> findDistinctModulesByAppId(@Param("appId") Long appId);
}
