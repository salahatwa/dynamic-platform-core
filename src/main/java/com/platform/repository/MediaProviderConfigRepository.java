package com.platform.repository;

import com.platform.entity.MediaFile;
import com.platform.entity.MediaProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaProviderConfigRepository extends JpaRepository<MediaProviderConfig, Long> {

    List<MediaProviderConfig> findByCorporateIdAndIsActiveOrderByIsDefaultDesc(Long corporateId, Boolean isActive);
    
    Optional<MediaProviderConfig> findByCorporateIdAndProviderTypeAndIsActive(
        Long corporateId, MediaFile.MediaProviderType providerType, Boolean isActive);
    
    Optional<MediaProviderConfig> findByCorporateIdAndIsDefaultAndIsActive(
        Long corporateId, Boolean isDefault, Boolean isActive);
    
    @Query("SELECT mpc FROM MediaProviderConfig mpc WHERE mpc.corporateId = :corporateId AND " +
           "(:appId IS NULL OR mpc.appId = :appId OR mpc.appId IS NULL) AND " +
           "mpc.isActive = true ORDER BY mpc.appId DESC NULLS LAST, mpc.isDefault DESC")
    List<MediaProviderConfig> findBestMatchingProvider(
        @Param("corporateId") Long corporateId, 
        @Param("appId") Long appId);
    
    boolean existsByCorporateIdAndProviderType(Long corporateId, MediaFile.MediaProviderType providerType);
}