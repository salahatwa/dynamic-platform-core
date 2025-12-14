package com.platform.repository;

import com.platform.entity.ErrorCodeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErrorCodeAuditRepository extends JpaRepository<ErrorCodeAudit, Long> {
    
    @Query("SELECT a FROM ErrorCodeAudit a WHERE a.errorCodeId = :errorCodeId ORDER BY a.changedAt DESC")
    List<ErrorCodeAudit> findByErrorCodeIdOrderByChangedAtDesc(@Param("errorCodeId") Long errorCodeId);
}
