package com.platform.repository;

import com.platform.entity.AppConfigGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppConfigGroupRepository extends JpaRepository<AppConfigGroup, Long> {
    
    List<AppConfigGroup> findByCorporateId(Long corporateId);
    
    List<AppConfigGroup> findByApp_Id(Long appId);
    
    List<AppConfigGroup> findByCorporateIdAndActive(Long corporateId, Boolean active);
    
    List<AppConfigGroup> findByApp_IdAndActive(Long appId, Boolean active);
    
    Optional<AppConfigGroup> findByGroupKeyAndApp_IdAndCorporateId(String groupKey, Long appId, Long corporateId);
    
    boolean existsByGroupKeyAndApp_IdAndCorporateId(String groupKey, Long appId, Long corporateId);
    
    // Dashboard count methods
    long countByApp_Id(Long appId);
    
    long countByCorporateId(Long corporateId);
    
    // App-name based methods for backward compatibility
    @Query("SELECT g FROM AppConfigGroup g WHERE g.app.name = :appName AND g.corporateId = :corporateId")
    List<AppConfigGroup> findByCorporateIdAndAppName(@Param("corporateId") Long corporateId, @Param("appName") String appName);
    
    @Query("SELECT g FROM AppConfigGroup g WHERE g.app.name = :appName AND g.corporateId = :corporateId AND g.active = :active")
    List<AppConfigGroup> findByCorporateIdAndAppNameAndActive(@Param("corporateId") Long corporateId, @Param("appName") String appName, @Param("active") Boolean active);
    
    @Query("SELECT g FROM AppConfigGroup g WHERE g.groupKey = :groupKey AND g.app.name = :appName AND g.corporateId = :corporateId")
    Optional<AppConfigGroup> findByGroupKeyAndAppNameAndCorporateId(@Param("groupKey") String groupKey, @Param("appName") String appName, @Param("corporateId") Long corporateId);
    
    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM AppConfigGroup g WHERE g.groupKey = :groupKey AND g.app.name = :appName AND g.corporateId = :corporateId")
    boolean existsByGroupKeyAndAppNameAndCorporateId(@Param("groupKey") String groupKey, @Param("appName") String appName, @Param("corporateId") Long corporateId);
}
