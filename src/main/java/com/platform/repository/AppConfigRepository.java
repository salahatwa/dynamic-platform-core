package com.platform.repository;

import com.platform.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
    
    List<AppConfig> findByCorporateId(Long corporateId);
    
    List<AppConfig> findByApp_Id(Long appId);
    
    List<AppConfig> findByApp_IdAndActive(Long appId, Boolean active);
    
    List<AppConfig> findByCorporateIdAndGroupId(Long corporateId, Long groupId);
    
    List<AppConfig> findByApp_IdAndIsPublic(Long appId, Boolean isPublic);
    
    Optional<AppConfig> findByConfigKeyAndApp_IdAndCorporateId(String configKey, Long appId, Long corporateId);
    
    boolean existsByConfigKeyAndApp_IdAndCorporateId(String configKey, Long appId, Long corporateId);
    
    @Query("SELECT DISTINCT a.name FROM AppConfig c JOIN c.app a WHERE c.corporateId = :corporateId")
    List<String> findDistinctAppNamesByCorporateId(Long corporateId);
    
    // Dashboard count methods
    long countByApp_Id(Long appId);
    
    long countByCorporateId(Long corporateId);
    
    // App-name based methods for backward compatibility
    @Query("SELECT c FROM AppConfig c WHERE c.app.name = :appName AND c.corporateId = :corporateId")
    List<AppConfig> findByCorporateIdAndAppName(@Param("corporateId") Long corporateId, @Param("appName") String appName);
    
    @Query("SELECT c FROM AppConfig c WHERE c.app.name = :appName AND c.corporateId = :corporateId AND c.active = :active")
    List<AppConfig> findByCorporateIdAndAppNameAndActive(@Param("corporateId") Long corporateId, @Param("appName") String appName, @Param("active") Boolean active);
    
    @Query("SELECT c FROM AppConfig c WHERE c.app.name = :appName AND c.corporateId = :corporateId AND c.isPublic = :isPublic")
    List<AppConfig> findByCorporateIdAndAppNameAndIsPublic(@Param("corporateId") Long corporateId, @Param("appName") String appName, @Param("isPublic") Boolean isPublic);
    
    @Query("SELECT c FROM AppConfig c WHERE c.configKey = :configKey AND c.app.name = :appName AND c.corporateId = :corporateId")
    Optional<AppConfig> findByConfigKeyAndAppNameAndCorporateId(@Param("configKey") String configKey, @Param("appName") String appName, @Param("corporateId") Long corporateId);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM AppConfig c WHERE c.configKey = :configKey AND c.app.name = :appName AND c.corporateId = :corporateId")
    boolean existsByConfigKeyAndAppNameAndCorporateId(@Param("configKey") String configKey, @Param("appName") String appName, @Param("corporateId") Long corporateId);
    
    // Content API methods
    Optional<AppConfig> findByConfigKeyAndApp_Id(String configKey, Long appId);
    
    @Query(value = "SELECT c.* FROM app_config c LEFT JOIN app_config_group g ON g.id = c.group_id WHERE " +
           "c.app_id = :appId " +
           "AND (:groupName IS NULL OR g.group_name = :groupName) " +
           "AND (:dataType IS NULL OR c.config_type = :dataType) " +
           "AND (:required IS NULL OR c.is_required = :required) " +
           "AND (:search IS NULL OR LOWER(c.config_key) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.config_key",
           countQuery = "SELECT COUNT(c.id) FROM app_config c LEFT JOIN app_config_group g ON g.id = c.group_id WHERE " +
           "c.app_id = :appId " +
           "AND (:groupName IS NULL OR g.group_name = :groupName) " +
           "AND (:dataType IS NULL OR c.config_type = :dataType) " +
           "AND (:required IS NULL OR c.is_required = :required) " +
           "AND (:search IS NULL OR LOWER(c.config_key) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<AppConfig> findAllWithFiltersForContent(
        @Param("appId") Long appId,
        @Param("groupName") String groupName,
        @Param("dataType") String dataType,
        @Param("required") Boolean required,
        @Param("search") String search,
        Pageable pageable
    );
    
    @Query(value = "SELECT DISTINCT g.group_name FROM app_config c " +
           "LEFT JOIN app_config_group g ON g.id = c.group_id " +
           "WHERE c.app_id = :appId AND g.group_name IS NOT NULL " +
           "ORDER BY g.group_name", nativeQuery = true)
    List<String> findDistinctGroupsByApp_Id(@Param("appId") Long appId);
    
    @Query(value = "SELECT DISTINCT c.config_type FROM app_config c " +
           "WHERE c.app_id = :appId AND c.config_type IS NOT NULL " +
           "ORDER BY c.config_type", nativeQuery = true)
    List<String> findDistinctDataTypesByApp_Id(@Param("appId") Long appId);
}
