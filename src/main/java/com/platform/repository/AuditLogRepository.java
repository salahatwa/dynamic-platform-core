package com.platform.repository;

import com.platform.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);
    Page<AuditLog> findByUserEmailOrderByTimestampDesc(String userEmail, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId, Pageable pageable);
    
    // Corporate-filtered queries
    Page<AuditLog> findByCorporateIdOrderByTimestampDesc(Long corporateId, Pageable pageable);
    Page<AuditLog> findByCorporateIdAndActionOrderByTimestampDesc(Long corporateId, String action, Pageable pageable);
    Page<AuditLog> findByCorporateIdAndEntityTypeOrderByTimestampDesc(Long corporateId, String entityType, Pageable pageable);
    Page<AuditLog> findByCorporateIdAndUserEmailOrderByTimestampDesc(Long corporateId, String userEmail, Pageable pageable);
    Page<AuditLog> findByCorporateIdAndEntityTypeAndEntityIdOrderByTimestampDesc(Long corporateId, String entityType, Long entityId, Pageable pageable);
}
