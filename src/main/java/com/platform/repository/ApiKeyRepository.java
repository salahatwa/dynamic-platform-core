package com.platform.repository;

import com.platform.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    @Query("SELECT ak FROM ApiKey ak " +
           "LEFT JOIN FETCH ak.user u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE ak.keyValue = :keyValue")
    Optional<ApiKey> findByKeyValueWithUser(@Param("keyValue") String keyValue);
    
    @Query("SELECT ak FROM ApiKey ak " +
           "LEFT JOIN FETCH ak.app " +
           "WHERE ak.keyValue = :keyValue")
    Optional<ApiKey> findByKeyValueWithApp(@Param("keyValue") String keyValue);
    
    Optional<ApiKey> findByKeyValue(String keyValue);
    List<ApiKey> findByUserId(Long userId);
    List<ApiKey> findByCorporateId(Long corporateId);
    List<ApiKey> findByUserIdAndActive(Long userId, Boolean active);
    
    // App-centric methods
    List<ApiKey> findByApp_Id(Long appId);
    List<ApiKey> findByApp_IdAndActive(Long appId, Boolean active);
    List<ApiKey> findByApp_IdAndCorporateId(Long appId, Long corporateId);
    List<ApiKey> findByApp_IdAndCorporateIdAndActive(Long appId, Long corporateId, Boolean active);
    
    // Dashboard count methods
    long countByApp_Id(Long appId);
    long countByApp_IdAndActive(Long appId, Boolean active);
    long countByCorporateId(Long corporateId);
}
