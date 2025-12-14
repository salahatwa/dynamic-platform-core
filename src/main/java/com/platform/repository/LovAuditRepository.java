package com.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.entity.LovAudit;

import java.util.List;

@Repository
public interface LovAuditRepository extends JpaRepository<LovAudit, Long> {
    
    List<LovAudit> findByLovIdOrderByTimestampDesc(Long lovId);
}
