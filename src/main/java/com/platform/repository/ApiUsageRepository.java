package com.platform.repository;

import com.platform.entity.ApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiUsageRepository extends JpaRepository<ApiUsage, Long> {
    
    /**
     * Find usage for corporate and month
     */
    Optional<ApiUsage> findByCorporateIdAndYearMonth(Long corporateId, String yearMonth);
    
    /**
     * Find usage for app and month
     */
    Optional<ApiUsage> findByAppIdAndYearMonth(Long appId, String yearMonth);
    
    /**
     * Find all usage for corporate
     */
    List<ApiUsage> findByCorporateIdOrderByYearMonthDesc(Long corporateId);
    
    /**
     * Find all usage for app
     */
    List<ApiUsage> findByAppIdOrderByYearMonthDesc(Long appId);
    
    /**
     * Increment request count for corporate and month
     */
    @Modifying
    @Query("UPDATE ApiUsage a SET a.requestCount = a.requestCount + 1, a.updatedAt = CURRENT_TIMESTAMP WHERE a.corporateId = :corporateId AND a.yearMonth = :yearMonth")
    int incrementRequestCount(@Param("corporateId") Long corporateId, @Param("yearMonth") String yearMonth);
    
    /**
     * Get total requests for corporate in current month
     */
    @Query("SELECT COALESCE(SUM(a.requestCount), 0) FROM ApiUsage a WHERE a.corporateId = :corporateId AND a.yearMonth = :yearMonth")
    long getTotalRequestsForMonth(@Param("corporateId") Long corporateId, @Param("yearMonth") String yearMonth);
}
