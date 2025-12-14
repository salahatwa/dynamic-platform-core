package com.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.platform.entity.Lov;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface LovRepository extends JpaRepository<Lov, Long> {
    
    List<Lov> findByCorporateId(Long corporateId);
    
    List<Lov> findByCorporateIdAndLovType(Long corporateId, String lovType);
    
    List<Lov> findByCorporateIdAndActive(Long corporateId, Boolean active);
    
    List<Lov> findByCorporateIdAndLovTypeAndActive(Long corporateId, String lovType, Boolean active);

    List<Lov> findByApp_Id(Long appId);
    
    List<Lov> findByApp_IdAndLovType(Long appId, String lovType);
    
    List<Lov> findByApp_IdAndActive(Long appId, Boolean active);
    
    List<Lov> findByApp_IdAndLovTypeAndActive(Long appId, String lovType, Boolean active);
    
    Optional<Lov> findByLovCodeAndCorporateId(String lovCode, Long corporateId);
    
    @Query("SELECT DISTINCT l.lovType FROM Lov l WHERE l.corporateId = :corporateId")
    List<String> findDistinctLovTypesByCorporateId(@Param("corporateId") Long corporateId);
    
    @Query("SELECT COUNT(l) FROM Lov l WHERE l.lovType = :lovType AND l.corporateId = :corporateId")
    Long countByLovTypeAndCorporateId(@Param("lovType") String lovType, @Param("corporateId") Long corporateId);
    
    // Dashboard count methods
    long countByApp_Id(Long appId);
    
    long countByCorporateId(Long corporateId);
    
    boolean existsByLovCodeAndCorporateId(String lovCode, Long corporateId);
    
    // App-based methods for app-centric architecture
    @Query("SELECT l FROM Lov l WHERE l.app.name = :appName AND l.corporateId = :corporateId")
    List<Lov> findByCorporateIdAndAppName(@Param("corporateId") Long corporateId, @Param("appName") String appName);
    
    @Query("SELECT l FROM Lov l WHERE l.app.name = :appName AND l.corporateId = :corporateId AND l.lovType = :lovType")
    List<Lov> findByCorporateIdAndAppNameAndLovType(@Param("corporateId") Long corporateId, @Param("appName") String appName, @Param("lovType") String lovType);
    
    @Query("SELECT l FROM Lov l WHERE l.app.name = :appName AND l.corporateId = :corporateId AND l.active = :active")
    List<Lov> findByCorporateIdAndAppNameAndActive(@Param("corporateId") Long corporateId, @Param("appName") String appName, @Param("active") Boolean active);
    
    @Query("SELECT l FROM Lov l WHERE l.app.name = :appName AND l.corporateId = :corporateId AND l.lovType = :lovType AND l.active = :active")
    List<Lov> findByCorporateIdAndAppNameAndLovTypeAndActive(@Param("corporateId") Long corporateId, @Param("appName") String appName, @Param("lovType") String lovType, @Param("active") Boolean active);
    
    // Content API methods
    Page<Lov> findByApp_Id(Long appId, Pageable pageable);
    Page<Lov> findByApp_IdAndActive(Long appId, Boolean active, Pageable pageable);
    Page<Lov> findByApp_IdAndLovCodeContainingIgnoreCase(Long appId, String lovCode, Pageable pageable);
    Page<Lov> findByApp_IdAndActiveAndLovCodeContainingIgnoreCase(Long appId, Boolean active, String lovCode, Pageable pageable);
    Optional<Lov> findByLovCodeAndApp_Id(String lovCode, Long appId);
    
    @Query("SELECT DISTINCT l.lovType FROM Lov l WHERE l.app.id = :appId ORDER BY l.lovType")
    List<String> findDistinctLovTypesByApp_Id(@Param("appId") Long appId);
}
