package com.platform.repository;

import com.platform.entity.ErrorCodeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorCodeCategoryRepository extends JpaRepository<ErrorCodeCategory, Long> {
    
    @Query("SELECT c FROM ErrorCodeCategory c WHERE c.corporateId = :corporateId OR c.corporateId IS NULL ORDER BY c.displayOrder, c.categoryName")
    List<ErrorCodeCategory> findByCorporateId(@Param("corporateId") Long corporateId);
    
    @Query("SELECT c FROM ErrorCodeCategory c WHERE (c.corporateId = :corporateId OR c.corporateId IS NULL) AND c.isActive = :isActive ORDER BY c.displayOrder, c.categoryName")
    List<ErrorCodeCategory> findByCorporateIdAndIsActive(@Param("corporateId") Long corporateId, @Param("isActive") Boolean isActive);
    
    @Query("SELECT c FROM ErrorCodeCategory c WHERE c.categoryCode = :categoryCode AND (c.corporateId = :corporateId OR c.corporateId IS NULL)")
    Optional<ErrorCodeCategory> findByCategoryCodeAndCorporateId(@Param("categoryCode") String categoryCode, @Param("corporateId") Long corporateId);
    
    boolean existsByCategoryCodeAndCorporateId(String categoryCode, Long corporateId);
    
    Optional<ErrorCodeCategory> findByIdAndCorporateId(Long id, Long corporateId);
    
    @Query("SELECT c FROM ErrorCodeCategory c WHERE c.corporateId = :corporateId OR c.corporateId IS NULL ORDER BY c.displayOrder, c.categoryName")
    List<ErrorCodeCategory> findByCorporateIdOrderByDisplayOrder(@Param("corporateId") Long corporateId);
    
    @Query("SELECT c FROM ErrorCodeCategory c WHERE (c.corporateId = :corporateId OR c.corporateId IS NULL) AND c.isActive = :isActive ORDER BY c.displayOrder, c.categoryName")
    List<ErrorCodeCategory> findByCorporateIdAndIsActiveOrderByDisplayOrder(@Param("corporateId") Long corporateId, @Param("isActive") Boolean isActive);
}
